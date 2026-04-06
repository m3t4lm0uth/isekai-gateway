package org.IFBX.isekaiGateway.isekaiBackendModern.triggerLogic;

import java.util.Collection;
import java.util.ServiceLoader;

import org.IFBX.isekaiGateway.api.triggerLogic.TriggerDefinition;
import org.IFBX.isekaiGateway.api.triggerLogic.TriggerRegistrar;

// static only helper to load TriggerDefinition implementations
public final class TriggerBootstrap {
    // ------- fields -------
    // constructor
    private TriggerBootstrap() {}

    // ------- methods -------
    // pass each discovered definition to the registry through registrar interface
    public static void loadAll(TriggerRegistrar registrar) {
        ServiceLoader.load(TriggerDefinition.class).forEach(registrar::register);
    }

    // overload for manual triggers
    public static void loadAll(TriggerRegistrar registrar, Collection<? extends TriggerDefinition> manualTriggers) {
        ServiceLoader.load(TriggerDefinition.class).forEach(registrar::register);
        manualTriggers.forEach(registrar::register);
    }
}
