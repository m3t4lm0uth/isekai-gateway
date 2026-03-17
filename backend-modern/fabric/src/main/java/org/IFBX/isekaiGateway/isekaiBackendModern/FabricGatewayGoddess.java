package org.IFBX.isekaiGateway.isekaiBackendModern;


import net.fabricmc.api.ModInitializer;
import org.IFBX.isekaiGateway.isekaiBackendModern.actionLogic.FabricHardcoreDeathTrigger;

public class FabricGatewayGoddess implements ModInitializer {
    
    @Override
    public void onInitialize() {
        // bootstrap the common mod.
        CommonClass.init();

        // register networking and triggers
        FabricGoddessProtocol.register();
        FabricHardcoreDeathTrigger.register();
    }
}
