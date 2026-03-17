package org.IFBX.isekaiGateway.commands;

import org.IFBX.isekaiGateway.GatewayDatabase;
import org.IFBX.isekaiGateway.GatewayMessenger;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Optional;
import java.util.UUID;

public class FlagSubHandler {

    private final ProxyServer server;
    private final GatewayDatabase database;
    private final GatewayMessenger messages;

    public FlagSubHandler(ProxyServer server, GatewayDatabase database, GatewayMessenger messages) {
        this.server = server;
        this.database = database;
        this.messages = messages;
    }

    
}
