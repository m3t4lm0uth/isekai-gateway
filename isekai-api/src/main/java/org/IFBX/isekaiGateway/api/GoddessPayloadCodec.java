package org.IFBX.isekaiGateway.api;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

// static-only utility class; defines how the payload is encoded
public class GoddessPayloadCodec {
    // ------- fields -------
    // loader neutral decoded rep.
    public record Message(byte opCode, UUID playerUuid, String actionKey) {}

    // constructor
    private GoddessPayloadCodec() {}

    // ------- main methods -------
    // build message with standard layout: [opCode][UUID msb][UUID lsb][actionKey UTF]
    public static byte[] buildMessage(byte opCode, UUID playerUuid, String actionKey) {
        // arg validation
        Objects.requireNonNull(playerUuid, "playerUuid must not be null.");
        if (actionKey == null || actionKey.isBlank()) {
            throw new IllegalArgumentException("actionKey must not be null or empty.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(baos)) {
            out.writeByte(opCode);
            out.writeLong(playerUuid.getMostSignificantBits());
            out.writeLong(playerUuid.getLeastSignificantBits());
            out.writeUTF(actionKey);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to build isekai-gateway plugin message.");
        }
        return baos.toByteArray();
    }

    // decode message from standard layout
    public static Message decode(DataInput in) throws IOException {
        byte opCode = in.readByte();
        long msb = in.readLong();
        long lsb = in.readLong();
        UUID playerUuid = new UUID(msb, lsb);
        String actionKey = in.readUTF();

        return new Message(opCode, playerUuid, actionKey);
    }
}