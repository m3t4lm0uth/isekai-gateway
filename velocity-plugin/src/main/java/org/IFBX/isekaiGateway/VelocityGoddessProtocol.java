package org.IFBX.isekaiGateway;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import org.IFBX.isekaiGateway.api.networking.GoddessMessage;
import org.IFBX.isekaiGateway.api.networking.GoddessPayloadCodec;
import org.IFBX.isekaiGateway.api.networking.GoddessProtocol;
import org.IFBX.isekaiGateway.exceptions.EventNotFoundException;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;
import org.slf4j.Logger;

// handle backend -> proxy gw plugin messages
public class VelocityGoddessProtocol {
    // ------- fields -------
    private final ChannelIdentifier channel;
    private final GatewayDatabase database;
    private final Logger logger;

    // constructor
    public VelocityGoddessProtocol(ChannelIdentifier channel, GatewayDatabase database, Logger logger) {
        this.channel = channel;
        this.database = database;
        this.logger = logger;
    }

    // ------- methods -------
    // read plugin message
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
        // ======= arg validation =======
        // only accept messages if for igw channel
        if (!event.getIdentifier().equals(channel)) {
            return;
        }

        // only accept messages from backend servers
        if (!(event.getSource() instanceof ServerConnection serverConnection)) {
            logger.debug(
                    "[isekai-gateway] Ignoring plugin message on {} from non-server source {}",
                    channel.getId(), event.getSource().getClass().getName()
                    );
            return;
        }

        // ======= decoding =======
        try (DataInputStream in = new DataInputStream(event.dataAsInputStream())) {
            // unwrap fabric's envelope: [variant length][inner bytes]
            int l = readVarInt(in);
            if (l < 0) {
                throw new IOException("[isekai-gateway] Negative payload length.");
            }

            // wrap in new DataInputStream for decoding
            byte[] inner = new byte[l];
            in.readFully(inner);

            // decode with shared codec
            GoddessMessage msg;
            try (DataInputStream goddessIn = new DataInputStream(new ByteArrayInputStream(inner))) {
                msg = GoddessPayloadCodec.decode(goddessIn);
            }

            // dispatch based on operationCode
            switch (msg.opCode()) {
                case GoddessProtocol.OP_SYNC_TRIGGERS -> {
                    var syncMsg = (GoddessPayloadCodec.SyncMessage) msg;
                    handleTriggerSync(serverConnection, syncMsg.triggerKeys());
                }
                case GoddessProtocol.OP_TRIGGER -> {
                    var playerMsg = (GoddessPayloadCodec.PlayerMessage) msg;
                    handleBackendTrigger(serverConnection, playerMsg.playerUuid(), playerMsg.triggerKey());
                }
                case GoddessProtocol.OP_CLEAR -> {
                    var playerMsg = (GoddessPayloadCodec.PlayerMessage) msg;
                    handleBackendClear(serverConnection, playerMsg.playerUuid(), playerMsg.triggerKey());
                }
                default -> logger.warn(
                        "[isekai-gateway] Received unknown operation code {} on plugin channel {} from server {}",
                        msg.opCode(), channel.getId(), serverConnection.getServerInfo().getName()
                );
            }

        } catch (IOException ex) {
            logger.error(
                    "[isekai-gateway] IO error while reading plugin message on {}: {}",
                    channel.getId(), ex.getMessage(), ex
            );
        } catch (Exception ex) {
            logger.error(
                    "[isekai-gateway] Failed to process plugin message on {}: {}",
                    channel.getId(), ex.getMessage(), ex
            );
        }
    }

    // ======= operation handlers =======
    // opCode sync
    private void handleTriggerSync(ServerConnection sourceServer, List<String> triggerKeys) {
        String backendName = sourceServer.getServerInfo().getName();
        try {
            database.syncTriggers(triggerKeys);
            logger.info(
                    "[isekai-gateway] Backend '{}' reported {} triggers: {}",
                    backendName, triggerKeys.size(), String.join(", ", triggerKeys)
            );
        } catch (GatewayDatabaseException ex) {
            logger.error(
                    "[isekai-gateway] Database error while syncing triggers from backend '{}': {}", backendName, ex.getMessage(), ex
            );
        }
    }

    // opCode trigger
    private void handleBackendTrigger(ServerConnection sourceServer, UUID playerUuid, String triggerKey) {

        String backendName = sourceServer.getServerInfo().getName();
        String username = sourceServer.getPlayer().getUsername();

        try {
            database.triggerMarkEventsRequired(playerUuid, triggerKey);
            logger.info(
                    "[isekai-gateway] Backend '{}' triggered event flags mapped to trigger key '{}' for player {} ({}).",
                    backendName, triggerKey, username, playerUuid
            );
        } catch (EventNotFoundException ex) {
            logger.warn(
                    "[isekai-gateway] Backend '{}' tried to trigger unknown or unmapped trigger key '{}' for player {} ({}).",
                    backendName, triggerKey, username, playerUuid
            );
        } catch (GatewayDatabaseException ex) {
            logger.error(
                    "[isekai-gateway] Database error when backend '{}' tried to trigger event flags mapped to trigger key '{}' for player {} ({}): {}.",
                    backendName, triggerKey, username, playerUuid, ex.getMessage(), ex
            );
        }
    }

    // opCode clear
    private void handleBackendClear(ServerConnection sourceServer, UUID playerUuid, String eventKey) {

        String backendName = sourceServer.getServerInfo().getName();
        String username = sourceServer.getPlayer().getUsername();

        try {
            database.clearEventRequired(playerUuid, eventKey);
            logger.info(
                    "[isekai-gateway] Backend '{}' cleared event flag '{}' for player {} ({}).",
                    backendName, eventKey, username, playerUuid
            );
        } catch (GatewayDatabaseException ex) {
            logger.error(
                    "[isekai-gateway] Database error when backend '{}' tried to clear event flag '{}' for player {} ({}): {}.",
                    backendName, eventKey, username, playerUuid, ex.getMessage(), ex
            );
        }
    }

    // ------- helpers -------
    // find VarInt
    private static int readVarInt(DataInputStream in) throws IOException {
        int numRead = 0;
        int result = 0;
        byte read;
        do {
            // read next raw byte from the stream
            read = in.readByte();

            // keep only the 7 low bits (0-6) that correspond to our data
            // shift them into correct position
            // first: shift 0
            // second: shift 7
            // third: shift 14
            // etc.
            int value = (read & 0b0111_1111);
            result |= (value << (7 * numRead));

            numRead ++;

            // guard against malformed / maliciously long encodings; VarInt is at most 5 bytes for a 32-bit int
            if (numRead > 5) {
                throw new IOException("[isekai-gateway] VarInt is too big.");
            }
        } while ((read & 0b1000_0000) != 0); // if high bit (bit 7) is set, there is another byte to be read

        return result;
    }
}
