package de.ownmods.settings.client;

import de.ownmods.settings.SettingsMod;
import de.ownmods.settings.api.ManagedMod;
import de.ownmods.settings.network.*;
import de.ownmods.settings.policy.*;
import java.util.*;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

/** Client preferences never write global rules. A disconnect drops all remote state. */
public final class ServerSettingsClient {
    private static Map<String, ModuleSchema> schemas = Map.of();
    private static Map<String, ModulePolicy> policies = Map.of();
    private static Map<String, PreferenceSnapshot> effective = Map.of();
    private static int revision, ticks;
    private static long request;
    private static volatile long connectionEpoch;
    private static int lastSendTick, attempts;
    private static String sent = "";
    private static boolean accepted, rejected;
    private ServerSettingsClient() { }
    public static void register() {
        ClientPlayConnectionEvents.INIT.register((handler, client) -> reset());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> reset());
        ClientPlayNetworking.registerGlobalReceiver(ServerStatePayload.TYPE, (packet, context) -> {
            long epoch = connectionEpoch;
            context.client().execute(() -> { if (epoch == connectionEpoch) receive(packet); });
        });
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (++ticks % 20 != 0 || client.player == null || schemas.isEmpty()
                    || !ClientPlayNetworking.canSend(PreferencesPayload.TYPE)) return;
            String values = SettingsWire.encode(ServerModules.personal(schemas));
            boolean changed = !values.equals(sent);
            boolean retry = !accepted && !rejected && attempts < 3 && ticks - lastSendTick >= 40;
            if (changed || retry) {
                if (changed) attempts = 0;
                attempts++; lastSendTick = ticks;
                sent = values; accepted = false; rejected = false;
                ClientPlayNetworking.send(new PreferencesPayload(SettingsWire.PROTOCOL, revision, request++, values));
            }
        });
    }
    private static void reset() {
        schemas = Map.of(); policies = Map.of(); effective = Map.of();
        connectionEpoch++; attempts = 0; lastSendTick = 0;
        revision = 0; ticks = 0; request = 0; sent = ""; accepted = false; rejected = false;
    }
    private static void receive(ServerStatePayload packet) {
        if (packet.protocol() != SettingsWire.PROTOCOL || packet.revision() <= 0 || packet.requestId() < -1) { reset(); return; }
        try {
            var local = ServerModules.schemas(); var remote = new TreeMap<String, ModuleSchema>();
            for (String id : packet.modules().split(",", -1)) {
                if (!local.containsKey(id) || remote.putIfAbsent(id, local.get(id)) != null)
                    throw new IllegalArgumentException("Unknown/duplicate server capability");
            }
            var nextPolicy = PolicyCodec.decode(packet.policy(), remote, true);
            var nextEffective = SettingsWire.decode(packet.effective(), remote);
            if (!nextEffective.keySet().equals(remote.keySet())) throw new IllegalArgumentException("Incomplete server state");
            if (revision != packet.revision() || packet.requestId() == -1) sent = "";
            schemas = Map.copyOf(remote); policies = nextPolicy; effective = nextEffective; revision = packet.revision();
            accepted = packet.accepted(); rejected = packet.requestId() >= 0 && !packet.accepted();
        } catch (IllegalArgumentException e) {
            SettingsMod.LOGGER.warn("Unsupported Vanilla Refined server settings: {}", e.getMessage()); reset();
        }
    }
    public static int revision() { return revision; }
    public static long nextActionId() { return request++; }
    public static boolean ready(String id) { return accepted && policies.containsKey(id); }
    public static boolean locked(ManagedMod mod, String key) {
        return policies.containsKey(mod.id()) && policies.get(mod.id()).locked(key);
    }
    public static boolean flag(ManagedMod mod, String key, boolean local) {
        var value = effective.get(mod.id()); return locked(mod, key) && value != null ? value.flag(key) : local;
    }
    public static String choice(ManagedMod mod, String key, String local) {
        var value = effective.get(mod.id()); return locked(mod, key) && value != null ? value.choice(key, local) : local;
    }
    public static String nextChoice(ManagedMod mod, String key, List<String> all, String current) {
        var p = policies.get(mod.id());
        List<String> allowed = p == null ? all : p.allowedChoices().getOrDefault(key, all);
        int index = allowed.indexOf(current);
        return allowed.get((index + 1) % allowed.size());
    }
    public static String hint(ManagedMod mod) {
        if (mod.clientOnly()) return Component.translatable("vr.server.local").getString();
        if (Minecraft.getInstance().level == null) return Component.translatable("ownmods.unsaved").getString();
        if (!policies.containsKey(mod.id())) return Component.translatable("vr.server.unavailable").getString();
        if (rejected) return Component.translatable("vr.server.rejected").getString();
        return Component.translatable(policies.get(mod.id()).mode() == ModulePolicy.Mode.PERSONAL
                ? "vr.server.personal" : "vr.server.locked").getString();
    }
}
