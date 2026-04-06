package org.IFBX.isekaiGateway.api.triggerLogic;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

// concrete trigger storage and lookup structure
public class TriggerRegistry implements TriggerRegistrar {
    private final Map<String, TriggerDefinition> triggers = new LinkedHashMap<>();

    // store trigger if not already defined
    @Override
    public void register(TriggerDefinition trigger) {
        String id = trigger.id();
        if (triggers.containsKey(id)) {
            throw new IllegalStateException("Duplicate trigger id: " + id);
        }
        triggers.put(id, trigger);
    }

    // returns one optional item by key
    public Optional<TriggerDefinition> get(String id) {
        return Optional.ofNullable(triggers.get(id));
    }
    // reflect current registry contents
    public Collection<TriggerDefinition> all() {
        return triggers.values();
    }
}
