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

import com.velocitypowered.api.proxy.server.RegisteredServer;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;
import org.slf4j.Logger;

import java.nio.file.Path;
import java.util.Optional;
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

        // init config events
        try {
            gatewayDatabase.initConfigEvents(gatewayConfig.getEventKeytoBackend());
        } catch (GatewayDatabaseException ex) {
            logger.error("Failed to initialize events from config.conf keys: {}", ex.getMessage(), ex);
        }
        
        // apply config backend mappings
        try {
            gatewayDatabase.applyBackendMappings(gatewayConfig.getEventKeytoBackend());
        } catch (GatewayDatabaseException ex) {
            logger.error("[isekai-gateway] Failed to apply backend mappings from config.conf: {}", ex.getMessage(), ex);
        }

        // register custom command
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
            String backendName = gatewayDatabase.chooseBackendForPlayer(uuid);

            if (backendName == null) {
                // no active req'd events / backend mappings, revert to normal routing
                return;
            }

            Optional<RegisteredServer> optionalServer = server.getServer(backendName);

            if (optionalServer.isEmpty()) {
                logger.warn("[isekai-gateway] Backend '{}' not found for player {}. Falling back to default routing.", backendName, player.getUsername());
                return;
            }

            event.setInitialServer(optionalServer.get());
            logger.info("[isekai-gateway] Routing player {} to backed '{}' based on event priorities.", player.getUsername(), backendName);

        } catch (GatewayDatabaseException ex) {
            // fallback to normal routing on DB error
            logger.error("[isekai-gateway] Failed to routing backend for {}: {}", player.getUsername(), ex.getMessage(), ex);
        }
    }
}