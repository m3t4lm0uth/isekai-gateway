package org.IFBX.isekaiGateway.api.networking;

import java.io.ByteArrayOutputStream;
import java.io.DataInput;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

// static-only utility class; defines how the payload is encoded
public class GoddessPayloadCodec {
    // ------- fields -------
    // loader neutral decoded rep.
    public record PlayerMessage(byte opCode, UUID playerUuid, String triggerKey) implements GoddessMessage {}
    public record SyncMessage(byte opCode, List<String> triggerKeys) implements GoddessMessage {}

    // constructor
    private GoddessPayloadCodec() {}

    // ------- main methods -------
    // ======= sync message =======
    // build message with standard layout: [OP_SYNC_TRIGGERS][count][triggerKey UTF ...]
    public static byte[] buildSyncMessage(List<String> triggerKeys) {
        // arg validation: ensure list object not null
        if (triggerKeys == null) {
            throw new IllegalArgumentException("triggerKeys must not be null.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = startMessage(baos, GoddessProtocol.OP_SYNC_TRIGGERS)) {
            out.writeInt(triggerKeys.size());
            for (String key : triggerKeys) {
                // arg validation: ensure each element within list object not null / blank
                if (key == null || key.isBlank()) {
                    throw new IllegalArgumentException("triggerKeys must not contain blank values.");
                }
                out.writeUTF(key);
            }
        } catch (IOException ex) {
            throw new RuntimeException("Failed to build isekai-gateway sync message.", ex);
        }
        return baos.toByteArray();
    }

    // ======= player flag message =======
    // build message with standard layout: [opCode][UUID msb][UUID lsb][triggerKey UTF]
    public static byte[] buildPlayerMessage(byte opCode, UUID playerUuid, String triggerKey) {
        // arg validation
        Objects.requireNonNull(playerUuid, "playerUuid must not be null.");
        if (triggerKey == null || triggerKey.isBlank()) {
            throw new IllegalArgumentException("triggerKey must not be null or empty.");
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (DataOutputStream out = startMessage(baos, opCode)) {
            out.writeLong(playerUuid.getMostSignificantBits());
            out.writeLong(playerUuid.getLeastSignificantBits());
            out.writeUTF(triggerKey);
        } catch (IOException ex) {
            throw new RuntimeException("Failed to build isekai-gateway plugin message.", ex);
        }
        return baos.toByteArray();
    }

    // ======= decode messages =======
    // decode handler
    public static GoddessMessage decode(DataInput in) throws IOException {
        byte opCode = in.readByte();
        return switch (opCode) {
            case GoddessProtocol.OP_SYNC_TRIGGERS -> decodeSyncPayload(opCode, in);
            case GoddessProtocol.OP_TRIGGER, GoddessProtocol.OP_CLEAR -> decodePlayerPayload(opCode, in);
            default -> throw new IOException("Unknown opcode: " + opCode);
        };
    }

    // decode sync payload
    private static SyncMessage decodeSyncPayload(byte opCode, DataInput in) throws IOException {
        int count = in.readInt();
        ArrayList<String> triggerKeys = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            triggerKeys.add(in.readUTF());
        }

        return new SyncMessage(opCode, List.copyOf(triggerKeys));
    }

    // decode player flag payload
    private static PlayerMessage decodePlayerPayload(byte opCode, DataInput in) throws IOException {
        long msb = in.readLong();
        long lsb = in.readLong();
        UUID playerUuid = new UUID(msb, lsb);
        String triggerKey = in.readUTF();

        return new PlayerMessage(opCode, playerUuid, triggerKey);
    }

    // ------- helpers -------
    // get opCode byte
    private static DataOutputStream startMessage(ByteArrayOutputStream baos, byte opCode) throws IOException {
        DataOutputStream out = new DataOutputStream(baos);
        out.writeByte(opCode);
        return out;
    }
}