package org.IFBX.isekaiGateway;

import org.IFBX.isekaiGateway.api.GoddessProtocol;
import org.IFBX.isekaiGateway.api.GoddessPayloadCodec;
import org.IFBX.isekaiGateway.exceptions.EventNotFoundException;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;

import org.slf4j.Logger;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.UUID;

// handle backend -> proxy gw plugin messages
public class VelocityGoddessProtocol {

    // var init
    private final ChannelIdentifier channel;
    private final GatewayDatabase database;
    private final Logger logger;

    // constructor
    public VelocityGoddessProtocol(ChannelIdentifier channel, GatewayDatabase database, Logger logger) {
        this.channel = channel;
        this.database = database;
        this.logger = logger;
    }

    // readVarInt helper
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

    // read plugin message
    @Subscribe
    public void onPluginMessage(PluginMessageEvent event) {
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
            GoddessPayloadCodec.Message msg;
            try (DataInputStream goddessIn = new DataInputStream(new ByteArrayInputStream(inner))) {
                msg = GoddessPayloadCodec.decode(goddessIn);
            }

            // dispatch based on operationCode
            byte operationCode = msg.opCode();
            UUID playerUuid = msg.playerUuid();
            String actionKey = msg.actionKey();
            switch (operationCode) {
                case GoddessProtocol.OP_TRIGGER -> handleBackendTrigger(serverConnection, playerUuid, actionKey);
                case GoddessProtocol.OP_CLEAR -> handleBackendClear(serverConnection, playerUuid, actionKey);
                default -> logger.warn(
                        "[isekai-gateway] Received unknown operationCode {} on plugin channel {} from server {}",
                        operationCode, channel.getId(), serverConnection.getServerInfo().getName()
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

    // opCode trigger
    private void handleBackendTrigger(ServerConnection sourceServer, UUID playerUuid, String actionKey) {

        String backendName = sourceServer.getServerInfo().getName();
        String username = sourceServer.getPlayer().getUsername();

        try {
            database.triggerMarkEventsRequired(playerUuid, actionKey);
            logger.info(
                    "[isekai-gateway] Backend '{}' triggered event flags mapped to action key '{}' for player {} ({}).",
                    backendName, actionKey, username, playerUuid
            );
        } catch (EventNotFoundException ex) {
            logger.warn(
                    "[isekai-gateway] Backend '{}' tried to trigger unknown or unmapped action key '{}' for player {} ({}).",
                    backendName, actionKey, username, playerUuid
            );
        } catch (GatewayDatabaseException ex) {
            logger.error(
                    "[isekai-gateway] Database error when backend '{}' tried to trigger event flags mapped to action key '{}' for player {} ({}): {}.",
                    backendName, actionKey, username, playerUuid, ex.getMessage(), ex
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
}
