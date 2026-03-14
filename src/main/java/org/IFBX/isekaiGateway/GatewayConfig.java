package org.IFBX.isekaiGateway;

import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GatewayConfig {
    private static final Logger logger = LoggerFactory.getLogger(GatewayConfig.class);
    private final Map<String, String> eventKeytoBackend;

    private GatewayConfig(Map<String, String> eventKeytoBackend) {
        this.eventKeytoBackend = eventKeytoBackend;
    }

    public static GatewayConfig load(Path dataDirectory, Logger logger) {
        File configFile = dataDirectory.resolve("config.conf").toFile();

        if (!configFile.exists()) {
            logger.warn("[isekai-gateway] config.conf not found in {}. No event mappings will be loaded.", dataDirectory.toAbsolutePath());
            return new GatewayConfig(Collections.emptyMap());
        }

        Config config = ConfigFactory.parseFile(configFile).resolve();

        Map<String, String> mapping = new HashMap<>();

        if (config.hasPath("events")) {
            List<? extends Config> events = config.getConfigList("events");
            for (Config ev : events) {
                String eventKey = ev.getString("event-key");
                String backend = ev.getString("backend");
                mapping.put(eventKey, backend);
            }
            logger.info("[isekai-gateway] Loaded {} event mappings from config.conf.", mapping.size());
        } else {
            logger.warn("[isekai-gateway] config.conf has no 'events' list; no mappings loaded.");
        }

        return new GatewayConfig(Collections.unmodifiableMap(mapping));
    }

    public Map<String, String> getEventKeytoBackend() {
        return eventKeytoBackend;
    }

    public String getBackendForEventKey(String eventKey) {
        return eventKeytoBackend.get(eventKey);
    }
}