package de.ownmods.inventorysort;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
public record SortRequestPayload(int protocol, int revision, long requestId, int menuId, int stateId, boolean container) implements CustomPacketPayload {
    public static final Type<SortRequestPayload> TYPE = new Type<>(Identifier.fromNamespaceAndPath("vanilla_refined", "sort_v1"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SortRequestPayload> CODEC = new StreamCodec<>() {
        public SortRequestPayload decode(RegistryFriendlyByteBuf b) {
            return new SortRequestPayload(b.readVarInt(), b.readVarInt(), b.readLong(), b.readVarInt(), b.readVarInt(), b.readBoolean());
        }
        public void encode(RegistryFriendlyByteBuf b, SortRequestPayload p) {
            b.writeVarInt(p.protocol); b.writeVarInt(p.revision); b.writeLong(p.requestId);
            b.writeVarInt(p.menuId); b.writeVarInt(p.stateId); b.writeBoolean(p.container);
        }
    };
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}
