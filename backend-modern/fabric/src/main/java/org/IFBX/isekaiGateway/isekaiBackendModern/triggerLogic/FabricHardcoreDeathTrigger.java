package org.IFBX.isekaiGateway.isekaiBackendModern.triggerLogic;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import org.IFBX.isekaiGateway.api.triggerLogic.TriggerDefinition;
import org.IFBX.isekaiGateway.isekaiBackendModern.Constants;
import org.IFBX.isekaiGateway.isekaiBackendModern.FabricGoddessProtocol;

// static-only utility class to register hardcore-death trigger for fabric backends
public class FabricHardcoreDeathTrigger implements TriggerDefinition {
    // ------- fields -------
    public static final String TRIGGER_KEY = "isekai:hardcore-death";

    // ------- main methods -------
    // set trigger key in definition
    @Override
    public String id() {
        return TRIGGER_KEY;
    }

    // register hardcore death trigger
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            // ignore non-player entities
            if (!(entity instanceof ServerPlayerEntity player)) {
                return;
            }

            FabricGoddessProtocol.sendTrigger(player, TRIGGER_KEY);

            // log for debugging
            Constants.LOG.info(
                    "[isekai-backend-fabric] Triggered event for player {} ({}).",
                    player.getGameProfile().getName(),
                    player.getUuid()
            );
        });
    }
}
