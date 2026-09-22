package de.ownmods.settings.network;

import de.ownmods.settings.policy.SettingsWire;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record PreferencesPayload(int protocol, int revision, long requestId, String values) implements CustomPacketPayload {
    public static final Type<PreferencesPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("vanilla_refined", "preferences_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, PreferencesPayload> CODEC = new StreamCodec<>() {
        @Override public PreferencesPayload decode(RegistryFriendlyByteBuf b) {
            return new PreferencesPayload(b.readVarInt(), b.readVarInt(), b.readLong(), b.readUtf(SettingsWire.MAX_CHARS));
        }
        @Override public void encode(RegistryFriendlyByteBuf b, PreferencesPayload p) {
            b.writeVarInt(p.protocol); b.writeVarInt(p.revision); b.writeLong(p.requestId); b.writeUtf(p.values, SettingsWire.MAX_CHARS);
        }
    };
    @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
