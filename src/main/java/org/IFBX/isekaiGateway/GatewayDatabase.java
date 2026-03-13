package org.IFBX.isekaiGateway;

import org.IFBX.isekaiGateway.exceptions.EventAlreadyExistsException;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;

import java.nio.charset.StandardCharsets;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

// class to centralize db access code
public class GatewayDatabase {
    private final HikariDataSource dataSource;
    private final Logger logger;

    // configure connection pool (HikariCP) using env vars from velocity container
    public GatewayDatabase(Logger logger) {
        this.logger = logger;

        String jdbcUrl = System.getenv("ISEKAI_DB_URL");
        String user = System.getenv("ISEKAI_DB_USER");
        String password = System.getenv("ISEKAI_DB_PASSWORD");

        if (jdbcUrl == null || user == null || password == null) {
            throw new IllegalStateException("[isekai-gateway] DB env var not set (ISEKAI_DB_URL / USER / PASSWORD).");
        }

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(jdbcUrl);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");

        // modest pool for a velocity proxy
        config.setMaximumPoolSize(5);
        config.setMinimumIdle(1);
        config.setConnectionTimeout(5000);
        config.setMaxLifetime(1800000);
        config.setPoolName("isekai-gateway-pool");

        this.dataSource = new HikariDataSource(config);
        logger.info("[isekai-gateway] Initialized HikariCP pool for {}.", jdbcUrl);
    }

    // helper: data holder
    public static record EventSummary(String eventKey, String name, String status) {}

    // helper: resolve event_id by event_key
    private Long findEventIdByKey(Connection conn, String eventKey) throws SQLException {
        String sql = """
                select id
                from isekai_gw.events
                where event_key = ?
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventKey);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getLong("id");
                }
                return null;
            }
        }
    }

    // list all events
    public List<EventSummary> listEvents() throws GatewayDatabaseException {
        String sql = """
                select event_key, name, status
                from isekai_gw.events
                order by created_at desc
                """;

        List<EventSummary> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql);
            ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String key = rs.getString("event_key");
                String name = rs.getString("name");
                String status = rs.getString("status");

                result.add(new EventSummary(key, name, status));
            }
        } catch (SQLException ex ) {
            throw new GatewayDatabaseException("Failed to list events", ex);
        }

        return result;
    }

    // create new event
    public void createEvent(String eventKey, String name) throws EventAlreadyExistsException, GatewayDatabaseException {
        String sql = """
                insert into isekai_gw.events (event_key, name)
                values (?, ?)
                """;

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, eventKey);
            ps.setString(2, name);
            ps.executeUpdate();
        } catch (SQLException ex) {
            String sqlState = ex.getSQLState();

            // integrity constraint violation -> event already exists
            if (sqlState != null && sqlState.startsWith("23")) {
                throw new EventAlreadyExistsException(eventKey);
            }

            // else, generic db failure
            throw new GatewayDatabaseException("Failed to create event: " + eventKey, ex);
        }
    }

    // find keys for (active) req'd event
    public List <String> findActiveEventRequiredKeys (UUID playerUuid) throws SQLException {
        String sql = """
                select e.event_key
                from isekai_gw.player_event_flags f
                join isekai_gw.events e
                  on e.id = f.event_id
                where f.player_uuid = ?
                  and f.required = true
                  and e.status = 'active'
                """;
        List<String> result = new ArrayList<>();

        try (Connection conn = dataSource.getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setObject(1, playerUuid);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(rs.getString("event_key"));
                }
            }
        }
        return result;
    }

    // flag player as event req'd, given event key
    public void markEventRequired(UUID playerUuid, String eventKey) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long eventId = findEventIdByKey(conn, eventKey);
                if (eventId == null) {
                    throw new SQLException("Event key not found: " + eventKey);
                }

                String sql = """
                        insert into isekai_gw.player_event_flags (
                            player_uuid, event_id, required, required_at, cleared_at
                        ) values (
                            ?, ?, true, current_timestamp, null
                        )
                        on conflict (player_uuid, event_id)
                        do update set
                            required = excluded.required,
                            required_at = excluded.required_at,
                            cleared_at = excluded.cleared_at,
                            updated_at = current_timestamp
                        """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, playerUuid);
                    ps.setLong(2, eventId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // clear event req'd flag for player
    public void clearEventRequired(UUID playerUuid, String eventKey) throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long eventId = findEventIdByKey(conn, eventKey);
                if (eventId == null) {
                    // nothing to clear
                    conn.commit();
                    return;
                }

                String sql = """
                        update isekai_gw.player_event_flags
                        set required = false,
                            cleared_at = current_timestamp,
                            updated_at = current_timestamp
                        where player_uuid = ?
                            and event_id = ?
                        """;

                try (PreparedStatement ps = conn.prepareStatement(sql)) {
                    ps.setObject(1, playerUuid);
                    ps.setLong(2, eventId);
                    ps.executeUpdate();
                }

                conn.commit();
            } catch (SQLException ex) {
                conn.rollback();
                throw ex;
            } finally {
                conn.setAutoCommit(true);
            }
        }
    }

    // cleanly close db (on proxy shutdown)
    public void close() {
        dataSource.close();
    }
}