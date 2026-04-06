package org.IFBX.isekaiGateway.api.triggerLogic;

// accepts defined triggers and stores within TriggerRegistry
public interface TriggerRegistrar {
    void register(TriggerDefinition trigger);
}
