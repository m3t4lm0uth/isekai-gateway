package org.IFBX.isekaiGateway;

import com.google.inject.Inject;

import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.plugin.annotation.DataDirectory;

import org.slf4j.Logger;

import java.nio.file.Path;
import java.sql.SQLException;
import java.util.UUID;

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
    private final Path dataDirectory;

    private final GatewayState state = new GatewayState();
    private final GatewayMessenger messages = new GatewayMessenger();
    private GatewayConfig gatewayConfig;
    private GatewayDatabase gatewayDatabase;

    // constructor, creates ProxyServer and Logger, loads directory
    @Inject
    public IsekaiGateway(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    // clean shutdown of db
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (gatewayDatabase != null) {
            gatewayDatabase.close();
        }
    }

    // create gw command when proxy initializes
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // load config first
        this.gatewayConfig = GatewayConfig.load(dataDirectory, logger);
        this.gatewayDatabase = new GatewayDatabase(logger);

        server.getCommandManager().register(
                "isekaigateway",
                new GatewayCommand(server, gatewayDatabase, messages),
                // alias
                "gw"
        );
        logger.info("[isekai-gateway] Isekai Gateway initialized. /isekaigateway command registered.");
    }

    // route flagged players to event server
    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        try {
            // look up (active) event req'd flags for player
            var requiredKeys = gatewayDatabase.findActiveEventRequiredKeys(uuid);

            if (requiredKeys.isEmpty()) {
                // none found, revert to normal routing
                return;
            }

            // take first event key
            String eventKey = requiredKeys.getFirst();
            String backendName = gatewayConfig.getBackendForEventKey(eventKey);

            if (backendName == null) {
                logger.warn("[isekai-gateway] Player {} has been flagged event required for '{}' but no backend mapping is configured.", player.getUsername(), eventKey);
                return;
            }

            // resolve backend and route player
            var optionalServer = server.getServer(backendName);

            if (optionalServer.isEmpty()) {
                logger.warn("[isekai-gateway] Backend '{}' for event '{}' not found; cannot route player {}.", backendName, eventKey, player.getUsername());
                return;
            }

            event.setInitialServer(optionalServer.get());
            logger.info("[isekai-gateway] Routing player {} to backed '{}' for event '{}'.", player.getUsername(), backendName, eventKey);

        } catch (SQLException ex) {
            // fallback to normal routing on DB error
            logger.error("[isekai-gateway] Failed to lookup event flags for {}: {}", player.getUsername(), ex.getMessage(), ex);
        }
    }
}