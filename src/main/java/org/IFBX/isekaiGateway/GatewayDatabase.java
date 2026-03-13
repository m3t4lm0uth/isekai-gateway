package org.IFBX.isekaiGateway;

import org.IFBX.isekaiGateway.exceptions.EventAlreadyExistsException;
import org.IFBX.isekaiGateway.exceptions.EventNotFoundException;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import org.slf4j.Logger;

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
            ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                String key = rs.getString("event_key");
                String name = rs.getString("name");
                String status = rs.getString("status");

                result.add(new EventSummary(key, name, status));
            }
        } catch (SQLException ex ) {
            throw new GatewayDatabaseException(
                    "Failed to list events", ex
            );
        }

        return result;
    }

    // create new event
    public void createEvent(String eventKey, String name) throws EventAlreadyExistsException, GatewayDatabaseException {
        String sql = """
                insert into isekai_gw.events (event_key, name)
                values (?, ?)
                """;

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)
        ) {
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
            throw new GatewayDatabaseException(
                    "Failed to create event: " + eventKey, ex
            );
        }
    }

    // update event status
    public void setEventStatus(String eventKey, String newStatus) throws EventNotFoundException, GatewayDatabaseException {
        String sql = """
                update isekai_gw.events
                set status = ?, updated_at = current_timestamp
                where event_key = ?
                """;

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ){
            ps.setString(1, newStatus);
            ps.setString(2, eventKey);
            int updated = ps.executeUpdate();

            if (updated == 0) {
                throw new EventNotFoundException(eventKey);
            }
        } catch (SQLException ex) {
            throw new GatewayDatabaseException(
                    "Failed to set status '" + newStatus + "' for event: " + eventKey, ex
            );
        }
    }

    // helper: activate event
    public void activateEvent(String eventKey) throws EventNotFoundException, GatewayDatabaseException {
        setEventStatus(eventKey, "active");
    }

    // helper: deactivate event
    public void deactivateEvent(String eventKey) throws EventNotFoundException, GatewayDatabaseException {
        setEventStatus(eventKey, "inactive");
    }

    // delete event
    public void deleteEvent(String eventKey) throws EventNotFoundException, GatewayDatabaseException {
        String sql = """
                delete from isekai_gw.events
                where event_key = ?
                """;

        try (Connection conn = dataSource.getConnection();
            PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, eventKey);
            int deleted = ps.executeUpdate();

            if (deleted == 0) {
                throw new EventNotFoundException(eventKey);
            }

            // if deleted > 0, any player_event_flags rows are removed by ON DELETE CASCADE.

        } catch (SQLException ex) {
            throw new GatewayDatabaseException(
                    "Failed to delete event with key: " + eventKey, ex
            );
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

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)
        ) {
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
    public void markEventRequired(UUID playerUuid, String eventKey) throws EventNotFoundException, GatewayDatabaseException {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Long eventId = findEventIdByKey(conn, eventKey);
                if (eventId == null) {
                    throw new EventNotFoundException(eventKey);
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
                throw new GatewayDatabaseException(
                        "Failed to mark event required for key: " + eventKey, ex
                );
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new GatewayDatabaseException(
                    "Failed to obtain connection.", ex
            );
        }
    }

    // clear event req'd flag for player
    public void clearEventRequired(UUID playerUuid, String eventKey) throws GatewayDatabaseException {
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
                throw new GatewayDatabaseException(
                        "Failed to clear event flag for key: " + eventKey, ex
                );
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException ex) {
            throw new GatewayDatabaseException(
                    "Failed to obtain connection.", ex
            );
        }
    }

    // cleanly close db (on proxy shutdown)
    public void close() {
        dataSource.close();
    }
}