package org.IFBX.isekaiGateway.isekaiBackendModern;


import net.fabricmc.api.ModInitializer;
import org.IFBX.isekaiGateway.api.triggerLogic.TriggerRegistry;
import org.IFBX.isekaiGateway.isekaiBackendModern.triggerLogic.FabricHardcoreDeathTrigger;
import org.IFBX.isekaiGateway.isekaiBackendModern.triggerLogic.TriggerBootstrap;

// main class
public class FabricGatewayGoddess implements ModInitializer {
    
    @Override
    public void onInitialize() {
        // bootstrap the common mod.
        CommonClass.init();

        // register networking
        FabricGoddessProtocol.register();

        // create registry and fill with trigger definitions
        TriggerRegistry registry = new TriggerRegistry(); // in-memory catalog of all triggers discovered on startup
        TriggerBootstrap.loadAll(registry);

        // register trigger event-listeners
        FabricHardcoreDeathTrigger.register();
    }
}
