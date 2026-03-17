package org.IFBX.isekaiGateway.commands;

import org.IFBX.isekaiGateway.GatewayDatabase;
import org.IFBX.isekaiGateway.GatewayMessenger;

public class EventSubHandler {
    private final GatewayDatabase database;
    private final GatewayMessenger messages;

    public EventSubHandler(GatewayDatabase database, GatewayMessenger messages) {
        this.database = database;
        this.messages = messages;
    }


}
