package org.IFBX.isekaiGateway.commands;

import java.util.Arrays;
import java.util.UUID;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.IFBX.isekaiGateway.GatewayDatabase;
import org.IFBX.isekaiGateway.GoddessMessenger;
import org.IFBX.isekaiGateway.PlayerFlagFilter;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

// commands for admins to list triggers / events / player flags in-game
public class ListSubHandler {
    // ------- fields -------
    private final ProxyServer server;
    private final GatewayDatabase database;
    private final GoddessMessenger messages;

    // constructor
    public ListSubHandler(ProxyServer server, GatewayDatabase database, GoddessMessenger messages) {
        this.server = server;
        this.database = database;
        this.messages = messages;
    }

    // ------- subcommand handler -------
    public void handleListSubcommand(CommandSource source, String[] args) {
        if (args.length < 1) {
            source.sendMessage(messages.listUsage());
            return;
        }

        String target = args[0];

        // subcommand triggers
        if (target.equalsIgnoreCase("triggers")) {
            handleListTriggers(source);
            return;
        }

        // subcommand events
        if (target.equalsIgnoreCase("events")) {
            handleListEvents(source);
            return;
        }

        // subcommand flags
        if (target.equalsIgnoreCase("flags")) {
            String[] flagArgs = Arrays.copyOfRange(args, 1, args.length);
            handleListFlags(source, flagArgs);
            return;
        }

        // else: command unknown
        source.sendMessage(messages.listUsage());
    }

    // ------ subcommand methods -------
    // list events
    private void handleListEvents(CommandSource source) {
        try {
            var events = database.listEvents();

            if (events.isEmpty()) {
                source.sendMessage(
                        Component.text("No events found.")
                );
                return;
            }

            source.sendMessage(
                    Component.text("Events:")
            );

            // build list display
            // format display: [status] name eventKey -> backend
            for (GatewayDatabase.EventSummary ev : events) {
                StringBuilder sb = new StringBuilder();
                sb.append('[').append(ev.status()).append("] ");
                sb.append("[prio = ").append(ev.priority()).append(']');
                sb.append(" [").append(ev.name()).append(']');
                sb.append(' ').append(ev.eventKey());

                if (ev.backend() != null && !ev.backend().isEmpty()) {
                    sb.append(" -> ").append(ev.backend());
                }

                if (ev.triggers() != null && !ev.triggers().isEmpty()) {
                    sb.append(" [triggers: ").append(ev.triggers()).append(']');
                }

                if (ev.clears() != null && !ev.clears().isEmpty()) {
                    sb.append(" [clears: ").append(ev.clears()).append(']');;
                }

                source.sendMessage(
                        Component.text(sb.toString())
                );
            }

        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while listing events: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }

    // list triggers
    private void handleListTriggers(CommandSource source) {
        try {
            var triggers = database.listTriggers();

            if (triggers.isEmpty()) {
                source.sendMessage(
                        Component.text("No triggers registered.")
                );
                return;
            }

            source.sendMessage(
                    Component.text("Triggers:")
            );

            // build list display
            for (GatewayDatabase.TriggerSummary a : triggers) {
                // format display
                StringBuilder sb = new StringBuilder();
                sb.append('[').append(a.triggerKey()).append(']');;
                if (a.description() != null && !a.description().isEmpty()) {
                    sb.append(" [description: ").append(a.description()).append(']');
                }
                if (a.events() != null && !a.events().isEmpty()) {
                    sb.append(" [mapped to: ").append(a.events()).append(']');
                }
                source.sendMessage(Component.text(sb.toString()));
            }

        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while listing triggers: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }

    // list player flags
    private void handleListFlags(CommandSource source, String[] args) {
        // parse mode: default ALL if no args
        PlayerFlagFilter filter;
        String label;
        if (args.length == 0) {
            filter = PlayerFlagFilter.ALL;
            label = "All player flags:";
        } else {
            String mode = args[0];
            if (mode.equalsIgnoreCase("all")) {
                filter = PlayerFlagFilter.ALL;
                label = "All player flags:";
            } else if (mode.equalsIgnoreCase("active")) {
                filter = PlayerFlagFilter.ACTIVE;
                label = "Active player flags:";
            } else {
                source.sendMessage(messages.listFlagsUsage());
                return;
            }
        }

        try {
            var flags = database.listPlayerFlags(filter);

            if (flags.isEmpty()) {
                source.sendMessage(
                        Component.text("No player flags found.")
                );
                return;
            }

            source.sendMessage(
                    Component.text(label)
            );

            // build list display
            for (GatewayDatabase.PlayerFlagSummary f : flags) {
                UUID uuid = f.playerUuid();
                // resolve name if online
                String name = server.getPlayer(uuid).map(Player::getUsername).orElse(uuid.toString());

                // format display
                StringBuilder sb = new StringBuilder();
                sb.append('[').append(name).append(']');
                sb.append(" [").append(f.eventKey()).append(']');
                sb.append(" [required =").append(f.required()).append(']');
                if (f.requiredAt() != null) {
                    sb.append(" [required at = ").append(f.requiredAt()).append(']');
                }
                if (f.clearedAt() != null) {
                    sb.append(" [cleared at = ").append(f.clearedAt()).append(']');
                }

                source.sendMessage(Component.text(sb.toString()));
            }

        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while listing player flags: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }
}
