package org.IFBX.isekaiGateway.isekaiBackendModern;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import org.IFBX.isekaiGateway.api.GoddessProtocol;

// fabric side wrapper for raw isekai-gateway payload bytes.
// implements 1.21 CustomPayload interface so can be sent with ServerPlayNetworking.
public record FabricGoddessPayload(byte[] data) implements CustomPayload {
    // ------- fields -------
    // channel id for this payload type, using shared CHANNEL_ID
    public static final CustomPayload.Id<FabricGoddessPayload> ID =
           new CustomPayload.Id(Identifier.of(GoddessProtocol.CHANNEL_ID));

    // ------- main methods -------
    // payload BytePacketBuf codec
    public static final PacketCodec<PacketByteBuf, FabricGoddessPayload> CODEC =
            new PacketCodec<PacketByteBuf, FabricGoddessPayload>() {
                @Override
                public FabricGoddessPayload decode(PacketByteBuf buf) {
                    byte[] bytes = buf.readByteArray();
                    return new FabricGoddessPayload(bytes);
                }

                @Override
                public void encode(PacketByteBuf buf, FabricGoddessPayload value) {
                    buf.writeByteArray(value.data());
                }
            };

    // type object to couple ID and CODEC
    public static final CustomPayload.Type<PacketByteBuf, FabricGoddessPayload> TYPE =
            new CustomPayload.Type<>(ID, CODEC);

    // req'd by CustomPayload: tell networking stack which channel this payload uses
    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }
}
