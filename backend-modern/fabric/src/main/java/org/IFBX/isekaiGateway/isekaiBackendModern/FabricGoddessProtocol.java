package org.IFBX.isekaiGateway.isekaiBackendModern;

import org.IFBX.isekaiGateway.api.GoddessProtocol;
import org.IFBX.isekaiGateway.api.GoddessPayloadCodec;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;

import net.minecraft.server.network.ServerPlayerEntity;

public class FabricGoddessProtocol {
    // static-only utility class
    private FabricGoddessProtocol() {}

    public static void register(){
        // register FabricGoddessPayload for server -> client (backend -> proxy) traffic
        PayloadTypeRegistry.playS2C().register(
                FabricGoddessPayload.ID,
                FabricGoddessPayload.CODEC
        );
    }

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
