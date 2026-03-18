package org.IFBX.isekaiGateway;

import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import com.google.inject.Inject;

import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.player.PlayerChooseInitialServerEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.ChannelIdentifier;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;

import org.slf4j.Logger;

import org.IFBX.isekaiGateway.api.GoddessProtocol;
import org.IFBX.isekaiGateway.commands.GatewayCommand;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

@Plugin(
        id = "isekai-gateway",
        name = "isekai-gateway",
        version = BuildConstants.VERSION,
        authors = {"m3t4lm0uth"}
)

// main class
public class IsekaiGateway {
    // ------- fields -------
    private static final ChannelIdentifier ISEKAI_CHANNEL = MinecraftChannelIdentifier.from(GoddessProtocol.CHANNEL_ID);

    private final ProxyServer server;
    private final Logger logger;
    private final Path dataDirectory;
    private final GoddessMessenger messages = new GoddessMessenger();

    private GatewayConfig gatewayConfig;
    private GatewayDatabase gatewayDatabase;

    // constructor
    @Inject
    public IsekaiGateway(ProxyServer server, Logger logger, @DataDirectory Path dataDirectory) {
        this.server = server;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    // ------- methods -------
    //  initialization
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        // ======= config =======
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

        // ======= messaging channel =======
        // create and register plugin messaging channel for receiving backend signals
        server.getChannelRegistrar().register(ISEKAI_CHANNEL);
        VelocityGoddessProtocol goddess = new VelocityGoddessProtocol(ISEKAI_CHANNEL, gatewayDatabase, logger);
        server.getEventManager().register(this, goddess);

        // ======= custom command =======
        // register /isekaigateway (alias /gw)
        server.getCommandManager().register(
                "isekaigateway",
                new GatewayCommand(server, gatewayDatabase, messages),
                // alias
                "gw"
        );
        logger.info("[isekai-gateway] Isekai Gateway initialized. /isekaigateway command registered.");
    }

    // server routing
    @Subscribe
    public void onChooseInitialServer(PlayerChooseInitialServerEvent event) {

        Player player = event.getPlayer();
        UUID uuid = player.getUniqueId();

        // find optimal backend and route flagged players
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
            logger.info("[isekai-gateway] Routing player {} to backend '{}' based on event priorities.", player.getUsername(), backendName);

        } catch (GatewayDatabaseException ex) {
            // fallback to normal routing on DB error
            logger.error("[isekai-gateway] Failed to route {} to backend: {}", player.getUsername(), ex.getMessage(), ex);
        }
    }

    // clean shutdown of db
    @Subscribe
    public void onProxyShutdown(ProxyShutdownEvent event) {
        if (gatewayDatabase != null) {
            gatewayDatabase.close();
        }
    }
}