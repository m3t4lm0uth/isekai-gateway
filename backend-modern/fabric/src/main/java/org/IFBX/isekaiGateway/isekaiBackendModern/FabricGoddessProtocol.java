package org.IFBX.isekaiGateway.isekaiBackendModern;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import org.IFBX.isekaiGateway.api.GoddessPayloadCodec;
import org.IFBX.isekaiGateway.api.GoddessProtocol;

// static-only utility class; message builder / sender
public class FabricGoddessProtocol {
    // ------- fields -------
    // constructor
    private FabricGoddessProtocol() {}

    // ------- main methods -------
    // register FabricGoddessPayload for server -> client (backend -> proxy) traffic
    public static void register(){
        PayloadTypeRegistry.playS2C().register(
                FabricGoddessPayload.ID,
                FabricGoddessPayload.CODEC
        );
    }

    // build, wrap, and send message
    public static void sendTrigger(ServerPlayerEntity player, String actionKey) {
        // build backend -> proxy message via shared codec
        byte[] payloadBytes = GoddessPayloadCodec.buildMessage(
                GoddessProtocol.OP_TRIGGER,
                player.getUuid(),
                actionKey
        );

        // wrap in fabric payload and send
        FabricGoddessPayload payload = new FabricGoddessPayload(payloadBytes);
        ServerPlayNetworking.send(player, payload);
    }
}
