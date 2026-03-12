package org.IFBX.isekaiGateway;

import com.google.inject.Inject;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import com.velocitypowered.api.proxy.Player;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;


@Plugin(
        id = "isekai-gateway",
        name = "isekai-gateway",
        version = BuildConstants.VERSION,
        authors = {"m3t4lm0uth"}
)

// main class
public class IsekaiGateway {

    private final ProxyServer server;
    private final Logger logger;
    private final GatewayState state = new GatewayState();

    // constructor, creates ProxyServer and Logger, loads directory
    @Inject
    public IsekaiGateway(ProxyServer server, Logger logger) {
        this.server = server;
        this.logger = logger;
    }

    // create gw command when proxy initializes
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        server.getCommandManager().register(
                "isekaigateway",
                new GatewayCommand(server, state),
                // alias
                "gw"
        );
        logger.info("Isekai Gateway initialized. /isekaigateway command registered.");
    }

    // route flagged players to event server
    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        if (state.isEventRequired(uuid)) {
            Optional<RegisteredServer> eventServer = server.getServer("event");
            if (eventServer.isPresent()) {
                event.setInitialServer(eventServer.get());
                logger.info("Routing flagged player {} to event server.", player.getUsername());
            } else {
                logger.warn("Event server 'event' not found; cannot route {}.", player.getUsername());
            }
        }
    }
}