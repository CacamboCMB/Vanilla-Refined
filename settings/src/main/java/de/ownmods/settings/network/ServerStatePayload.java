package de.ownmods.settings.network;

import de.ownmods.settings.policy.PolicyCodec;
import de.ownmods.settings.policy.SettingsWire;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** Server -> one player. Does not contain other players' settings or identities. */
public record ServerStatePayload(int protocol, int revision, long requestId, boolean accepted,
                                 String modules, String policy, String effective) implements CustomPacketPayload {
    public static final Type<ServerStatePayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("vanilla_refined", "server_state_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ServerStatePayload> CODEC = new StreamCodec<>() {
        @Override public ServerStatePayload decode(RegistryFriendlyByteBuf b) {
            return new ServerStatePayload(b.readVarInt(), b.readVarInt(), b.readLong(), b.readBoolean(),
                    b.readUtf(1024), b.readUtf(PolicyCodec.MAX_CHARS), b.readUtf(SettingsWire.MAX_CHARS));
        }
        @Override public void encode(RegistryFriendlyByteBuf b, ServerStatePayload p) {
            b.writeVarInt(p.protocol); b.writeVarInt(p.revision); b.writeLong(p.requestId); b.writeBoolean(p.accepted);
            b.writeUtf(p.modules, 1024); b.writeUtf(p.policy, PolicyCodec.MAX_CHARS); b.writeUtf(p.effective, SettingsWire.MAX_CHARS);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
