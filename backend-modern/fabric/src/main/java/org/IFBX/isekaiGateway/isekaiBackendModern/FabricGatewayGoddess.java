package org.IFBX.isekaiGateway.isekaiBackendModern;


import net.fabricmc.api.ModInitializer;
import org.IFBX.isekaiGateway.isekaiBackendModern.actionLogic.FabricHardcoreDeathAction;

public class FabricGatewayGoddess implements ModInitializer {
    
    @Override
    public void onInitialize() {
        // bootstrap the common mod.
        CommonClass.init();

        // register networking and actions
        FabricGoddessProtocol.register();
        FabricHardcoreDeathAction.register();
    }
}
