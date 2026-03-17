package org.IFBX.isekaiGateway;

import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

// class for managing player flags
public class GatewayState {
    private final Set<UUID> eventRequiredPlayers = ConcurrentHashMap.newKeySet();

    // add player
    public void markEventRequired(UUID playerId) {
        eventRequiredPlayers.add(playerId);
    }

    // remove player
    public void clearEventRequired(UUID playerId) {
        eventRequiredPlayers.remove(playerId);
    }

    // check if player flagged
    public boolean isEventRequired(UUID playerId) {
        return eventRequiredPlayers.contains(playerId);
    }
}
