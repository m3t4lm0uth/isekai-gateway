package org.IFBX.isekaiGateway.isekaiBackendModern.actionLogic;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.IFBX.isekaiGateway.isekaiBackendModern.CommonActions;
import org.IFBX.isekaiGateway.isekaiBackendModern.Constants;
import org.IFBX.isekaiGateway.isekaiBackendModern.FabricGoddessProtocol;

public class FabricHardcoreDeathTrigger {
    // static-only utility class
    private FabricHardcoreDeathTrigger() {}

    // register hardcore death trigger
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // ignore non-player entities
            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }

            FabricGoddessProtocol.sendTrigger(player, CommonActions.ACTION_HARDCORE_DEATH);

            // log for debugging
            Constants.LOG.info(
                    "[isekai-backend-fabric] Triggered event for player {} ({}).",
                    player.getGameProfile().getName(),
                    player.getUuid()
            );
        });
    }
}
