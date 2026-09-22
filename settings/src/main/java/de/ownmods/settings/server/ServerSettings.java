package de.ownmods.settings.server;

import de.ownmods.settings.SettingsMod;
import de.ownmods.settings.api.ManagedMod;
import de.ownmods.settings.network.*;
import de.ownmods.settings.policy.*;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Server-thread-owned state, scoped by server identity and authenticated player UUID. */
public final class ServerSettings {
    private static final Map<MinecraftServer, Session> SESSIONS = new IdentityHashMap<>();
    private ServerSettings() { }
    public static void register() {
        PayloadTypeRegistry.serverboundPlay().register(PreferencesPayload.TYPE, PreferencesPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ServerStatePayload.TYPE, ServerStatePayload.CODEC);
        ServerLifecycleEvents.SERVER_STARTING.register(server -> SESSIONS.put(server, new Session(server)));
        ServerLifecycleEvents.SERVER_STOPPED.register(SESSIONS::remove);
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            Session session = SESSIONS.get(server);
            if (session != null && ++session.ticks % 100 == 0) session.checkPolicyReload();
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            Session session = SESSIONS.get(server);
            if (session != null) {
                session.join(handler.player);
                session.send(handler.player, -1, false);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            Session session = SESSIONS.get(server);
            if (session != null) session.players.remove(handler.player.getUUID());
        });
        ServerPlayNetworking.registerGlobalReceiver(PreferencesPayload.TYPE, (packet, context) ->
                context.server().execute(() -> {
                    Session session = SESSIONS.get(context.server());
                    if (session != null) session.accept(context.player(), packet);
                }));
    }
    public static PreferenceSnapshot effective(ServerPlayer player, ManagedMod module) {
        Session session = SESSIONS.get(player.level().getServer());
        if (session == null || !session.schemas.containsKey(module.id())) return ModuleSchema.from(module).defaults().disabled();
        PlayerState state = session.players.get(player.getUUID());
        return session.policies.get(module.id()).effective(session.schemas.get(module.id()),
                state == null || !state.active ? null : state.values.get(module.id()));
    }
    /** Shared authority/replay check for custom inventory actions. */
    public static boolean allowAction(ServerPlayer player, int revision, long requestId) {
        Session session = SESSIONS.get(player.level().getServer());
        if (session == null || revision != session.revision || player.isRemoved() || !player.isAlive() || player.isSpectator()) return false;
        PlayerState state = session.players.get(player.getUUID());
        return state != null && state.active && state.actions.allow(requestId, System.nanoTime());
    }
    private static final class PlayerState {
        Map<String, PreferenceSnapshot> values = Map.of();
        boolean active;
        final RequestGate preferences = new RequestGate(500_000_000L);
        final RequestGate actions = new RequestGate(150_000_000L);
    }
    private static final class Session {
        final MinecraftServer server;
        final Map<String, ModuleSchema> schemas = ServerModules.schemas();
        final Map<UUID, PlayerState> players = new HashMap<>();
        final Path root, policyFile;
        Map<String, ModulePolicy> policies;
        int revision = 1, ticks;
        long lastModified = Long.MIN_VALUE;
        String policyText;
        Session(MinecraftServer server) {
            this.server = server;
            root = server.getWorldPath(LevelResource.ROOT).resolve("vanilla_refined");
            policyFile = server.isDedicatedServer()
                    ? FabricLoader.getInstance().getConfigDir().resolve("vanilla_refined/server-policy.properties")
                    : root.resolve("server-policy.properties");
            policies = PolicyCodec.defaults(schemas, server.isDedicatedServer());
            try {
                if (!Files.exists(policyFile)) PolicyCodec.atomicWrite(policyFile, PolicyCodec.encode(policies));
                policies = PolicyCodec.decode(PolicyCodec.readBounded(policyFile, PolicyCodec.MAX_CHARS), schemas, server.isDedicatedServer());
                lastModified = Files.getLastModifiedTime(policyFile).toMillis();
            } catch (IOException | IllegalArgumentException e) {
                var closed = new TreeMap<String, ModulePolicy>();
                policies.forEach((id, p) -> closed.put(id, new ModulePolicy(ModulePolicy.Mode.DISABLED, p.serverValues(), p.booleanRules(), p.allowedChoices())));
                policies = Map.copyOf(closed);
                SettingsMod.LOGGER.error("Invalid server policy; ALL server features disabled. Original file retained: {}", policyFile, e);
            }
            policyText = PolicyCodec.encode(policies);
            SettingsMod.LOGGER.info("Vanilla Refined server authority ready: {} modules; policy {}", schemas.size(), policyFile);
        }
        void join(ServerPlayer player) {
            var state = new PlayerState();
            Path file = preferencesFile(player);
            if (Files.exists(file)) try {
                state.values = SettingsWire.decode(PolicyCodec.readBounded(file, SettingsWire.MAX_CHARS), schemas);
            } catch (IOException | IllegalArgumentException e) {
                SettingsMod.LOGGER.warn("Player preferences could not be read; automation remains opt-in for {}", player.getUUID());
            }
            // A saved file alone never opts a newly connected, unmodded client into personal automation.
            players.put(player.getUUID(), state);
        }
        Path preferencesFile(ServerPlayer player) { return root.resolve("players").resolve(player.getUUID() + ".settings"); }
        void accept(ServerPlayer player, PreferencesPayload packet) {
            if (player.isRemoved()) return;
            PlayerState state = players.get(player.getUUID());
            if (state == null || !state.preferences.allow(packet.requestId(), System.nanoTime())) return;
            if (packet.protocol() != SettingsWire.PROTOCOL || packet.revision() != revision) {
                send(player, packet.requestId(), false); return;
            }
            try {
                Map<String, PreferenceSnapshot> next = SettingsWire.decode(packet.values(), schemas);
                if (!next.keySet().equals(schemas.keySet())) throw new IllegalArgumentException("Incomplete preferences");
                // Publish only after the atomic file replacement succeeds. Never write client values into global policy.
                PolicyCodec.atomicWrite(preferencesFile(player), SettingsWire.encode(next));
                state.values = next; state.active = true;
                send(player, packet.requestId(), true);
            } catch (IOException | IllegalArgumentException e) {
                SettingsMod.LOGGER.warn("Rejected personal settings for {}: {}", player.getUUID(), e.getMessage());
                send(player, packet.requestId(), false);
            }
        }
        void send(ServerPlayer player, long request, boolean accepted) {
            if (!ServerPlayNetworking.canSend(player, ServerStatePayload.TYPE)) return;
            var effective = new TreeMap<String, PreferenceSnapshot>();
            var state = players.get(player.getUUID());
            schemas.forEach((id, schema) -> effective.put(id, policies.get(id).effective(schema,
                    state == null || !state.active ? null : state.values.get(id))));
            ServerPlayNetworking.send(player, new ServerStatePayload(SettingsWire.PROTOCOL, revision, request, accepted,
                    String.join(",", new TreeSet<>(schemas.keySet())), policyText, SettingsWire.encode(effective)));
        }
        void checkPolicyReload() {
            try {
                if (!Files.exists(policyFile)) return;
                long modified = Files.getLastModifiedTime(policyFile).toMillis();
                if (modified == lastModified) return;
                lastModified = modified;
                var next = PolicyCodec.decode(PolicyCodec.readBounded(policyFile, PolicyCodec.MAX_CHARS), schemas, server.isDedicatedServer());
                String nextText = PolicyCodec.encode(next);
                if (nextText.equals(policyText)) return;
                policies = next; policyText = nextText; revision++;
                for (var player : server.getPlayerList().getPlayers()) send(player, -1, false);
                SettingsMod.LOGGER.info("Server policy reloaded at revision {}", revision);
            } catch (IOException | IllegalArgumentException e) {
                SettingsMod.LOGGER.error("Invalid policy edit rejected; previous effective server policy remains active", e);
            }
        }
    }
}
