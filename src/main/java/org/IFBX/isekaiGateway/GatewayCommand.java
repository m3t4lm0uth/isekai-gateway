package org.IFBX.isekaiGateway;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import org.IFBX.isekaiGateway.exceptions.EventAlreadyExistsException;
import org.IFBX.isekaiGateway.exceptions.EventNotFoundException;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

import java.util.Arrays;
import java.util.Optional;

// class to build command for admins to manipulate player flags / events in-game
public class GatewayCommand implements SimpleCommand {

    private final ProxyServer server;
    private final GatewayDatabase database;
    private final GatewayMessenger messages;

    // constructor: allows other methods to use server/database
    public GatewayCommand(ProxyServer server, GatewayDatabase database, GatewayMessenger messages) {
        this.server = server;
        this.database = database;
        this.messages = messages;
    }

    // helper: support for multi-word names
    private static String joinArgs(String[] args, int startIndex) {
        StringBuilder sb = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                sb.append(' ');
            }
            sb.append(args[i]);
        }
        return sb.toString();
    }

    // ------- arg helpers -------
    // root args
    private void sendRootUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway <trigger | clear | event> ...")
        );
    }

    // flag args
    private void sendFlagUsage(CommandSource source, String subcommand) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway " + subcommand.toLowerCase() + " <player> <event_key>")
        );
    }

    // event root args
    private void sendEventUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway event <create | activate | deactivate | delete | map | rename | priority | list>")
        );
    }

    // event create args
    private void sendCreateUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway event create <event_key> <name>")
        );
    }

    // event status update args
    private void sendStatusUpdateUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway event <activate|deactivate> <event_key>")
        );
    }

    // event delete args
    private void sendDeleteUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway event delete <event_key>")
        );
    }

    // event map args
    private void sendMapUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway event map <event_key> <backend>")
        );
    }

    // event rename args
    private void sendRenameUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway event rename <event_key> <new_name>")
        );
    }

    // event priority args
    private void sendPriorityUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway event priority <event_key> <priority>")
        );
    }

    // ------- main methods --------
    // execute: /gw is ran.
    @Override
    public void execute(Invocation invocation) {

        CommandSource source = invocation.source(); // who ran the command
        String[] args = invocation.arguments(); // array of strings following command

        // ------- arg handling -------
        // no args
        if (args.length == 0) {
            sendRootUsage(source);
            return;
        }

        String subcommand = args [0];

        // event subcommand
        if (subcommand.equalsIgnoreCase("event")) {
            String[] eventArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventSubcommand(source, eventArgs);
            return;
        }

        // flag subcommands
        if (subcommand.equalsIgnoreCase("trigger") || subcommand.equalsIgnoreCase("clear")) {
            String[] flagArgs = Arrays.copyOfRange(args, 1, args.length);
            handleFlagSubcommand(source, subcommand, flagArgs);
            return;
        }

        // unknown subcommand
        sendRootUsage(source);
    }

    // ------- player flag actions -------
    // helper: handle when subcommand = trigger || clear
    private void handleFlagSubcommand(CommandSource source, String subcommand, String[] args) {
        if (args.length != 2) {
            sendFlagUsage(source, subcommand);
            return;
        }

        String targetName = args[0];
        String eventKey = args[1];

        // resolve target player
        Optional<Player> optionalPlayer = server.getPlayer(targetName);

        // stops execution if player not found
        if (optionalPlayer.isEmpty()) {
            source.sendMessage (Component.text("Player not found: " + targetName));
            return;
        }

        // set target to actual player
        Player target = optionalPlayer.get();

        // trigger subcommand
        if (subcommand.equalsIgnoreCase("trigger")){
            // flag as event-req'd
            try {
                database.markEventRequired(target.getUniqueId(), eventKey);

                // disconnect message and notify source
                Component message = messages.buildEventTriggeredMessage();
                target.disconnect(message);
                source.sendMessage(
                        Component.text("Triggered event '" + eventKey + "' for " + target.getUsername())
                );
            } catch (EventNotFoundException ex) {
                source.sendMessage(
                        Component.text("No event found with key '" + eventKey + "'.")
                                .color(NamedTextColor.RED)
                );
            } catch (GatewayDatabaseException ex) {
                source.sendMessage(
                        Component.text("Database error while triggering event flag: " + ex.getMessage())
                                .color(NamedTextColor.RED)
                );
            }

        // clear subcommand
        } else if (subcommand.equalsIgnoreCase("clear")) {
            //clear the flag
            try {
                database.clearEventRequired(target.getUniqueId(), eventKey);
                source.sendMessage(
                        Component.text("Cleared event '" + eventKey + "' flag for " + target.getUsername())
                );
            } catch (GatewayDatabaseException ex) {
                source.sendMessage(
                        Component.text("Database error while clearing event flag: " + ex.getMessage())
                        .color(NamedTextColor.RED)
                );
            }

        } else {
            // unknown subcommand
            sendFlagUsage(source, subcommand);
        }
    }

    // ------- event actions -------
    // helper: handle when subcommand = event
    private void handleEventSubcommand(CommandSource source, String[] args) {
        if (args.length < 1) {
            sendEventUsage(source);
            return;
        }

        String action = args[0];

        // subcommand create
        if (action.equalsIgnoreCase("create")) {
            String[] createArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventCreate(source, createArgs);
            return;
        }

        // subcommand activate | deactivate
        if (action.equalsIgnoreCase("activate") || action.equalsIgnoreCase("deactivate")) {
            handleEventStatusUpdate(source, args);
            return;
        }

        // subcommand delete
        if (action.equalsIgnoreCase("delete")) {
            String[] deleteArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventDelete(source, deleteArgs);
            return;
        }

        //subcommand map
        if (action.equalsIgnoreCase("map")) {
            String[] mapArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventMap(source, mapArgs);
            return;
        }

        //subcommand rename
        if (action.equalsIgnoreCase("rename")) {
            String[] renameArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventRename(source, renameArgs);
            return;
        }

        //subcommand priority
        if (action.equalsIgnoreCase("priority")) {
            String[] priorityArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventPriority(source, priorityArgs);
            return;
        }

        // subcommand list
        if (action.equalsIgnoreCase("list")) {
            handleEventList(source);
            return;
        }

        // else: command unknown
        sendEventUsage(source);
    }

    // event list
    private void handleEventList(CommandSource source) {
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
            for (GatewayDatabase.EventSummary ev : events) {
                // example output format: [status] eventKey -> backend - name
                String formatBackend = (ev.backend() != null && !ev.backend().isEmpty())
                        ? " -> " + ev.backend()
                        : "";

                String line = String.format(
                        "[%s] [prio = %d] %s%s - %s",
                        ev.status(),
                        ev.priority(),
                        ev.name(),
                        ev.eventKey(),
                        formatBackend
                );
                source.sendMessage(
                        Component.text(line)
                );
            }
        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while listing events: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }

    // create event -args: <event_key> <name...>
    private void handleEventCreate(CommandSource source, String[] args) {
        if (args.length < 2) {
            sendCreateUsage(source);
            return;
        }

        String eventKey = args[0];
        String name = joinArgs(args, 1);

        try {
            database.createEvent(eventKey, name);
            source.sendMessage(
                    Component.text("Created event '" + eventKey + "' with name '" + name + "'.")
            );
        } catch (EventAlreadyExistsException ex) {
            source.sendMessage(
                    Component.text("Event with key '" + eventKey + "' already exists.")
                            .color(NamedTextColor.RED)
            );

        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while creating event: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }

    // activate || deactivate event
    private void handleEventStatusUpdate(CommandSource source, String[] args) {
        if (args.length != 2) {
            sendStatusUpdateUsage(source);
            return;
        }

        String action = args[0];
        String eventKey = args[1];

        try {
            if (action.equalsIgnoreCase("activate")) {
                database.activateEvent(eventKey);
                source.sendMessage(
                        Component.text("Activated event '" + eventKey + "'.")
                );
                return;
            } else if (action.equalsIgnoreCase("deactivate")) {
                database.deactivateEvent(eventKey);
                source.sendMessage(
                        Component.text("Deactivated event '" + eventKey + "'.")
                );
                return;
            }

        } catch (EventNotFoundException ex) {
            source.sendMessage(
                    Component.text("No event found with key '" + eventKey + "'.")
                            .color(NamedTextColor.RED)
            );
        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while updating status of '" + eventKey + "'.")
                            .color(NamedTextColor.RED)
            );
        }
    }

    // delete event
    private void handleEventDelete(CommandSource source, String[] args) {
        if (args.length != 1) {
            sendDeleteUsage(source);
            return;
        }

        String eventKey = args[0];

        try {
            database.deleteEvent(eventKey);
            source.sendMessage(
                    Component.text("Deleted event '" + eventKey + "'.")
            );
        } catch (EventNotFoundException ex) {
            source.sendMessage(
                    Component.text("No event found with key '" + eventKey + "'.")
                            .color(NamedTextColor.RED)
            );
        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while deleting event: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }

    // map event backend
    private void handleEventMap(CommandSource source, String[] args) {
        if (args.length != 2) {
            sendMapUsage(source);
            return;
        }

        String eventKey = args[0];
        String backend = args[1];

        try {
            database.mapEventBackend(eventKey, backend);
            source.sendMessage(
                    Component.text("Mapped event '" + eventKey + "' to backend '" + backend + "'.")
            );
        } catch (EventNotFoundException ex) {
            source.sendMessage(
                    Component.text("No event found with key '" + eventKey + "'.")
                            .color(NamedTextColor.RED)
            );
        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while mapping backend: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }

    // rename event
    private void handleEventRename(CommandSource source, String[] args) {
        if (args.length < 2) {
            sendRenameUsage(source);
            return;
        }

        String eventKey = args[0];
        String newName = joinArgs(args, 1);

        try {
            database.renameEvent(eventKey, newName);
            source.sendMessage(
                    Component.text("Renamed event '" + eventKey + "' to '" + newName + "'.")
            );
        } catch (EventNotFoundException ex) {
            source.sendMessage(
                    Component.text("No event found with key '" + eventKey + "'.")
                            .color(NamedTextColor.RED)
            );
        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while renaming event: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }

    // set event priority
    private void handleEventPriority(CommandSource source, String[] args) {
        if (args.length < 2) {
            sendPriorityUsage(source);
            return;
        }

        String eventKey = args[0];
        String rawPrio = args[1];
        int priority;

        try {
            priority = Integer.parseInt(rawPrio);
        } catch (NumberFormatException ex) {
            source.sendMessage(
                    Component.text("Priority must be an integer, got '" + rawPrio + "'.")
                            .color(NamedTextColor.RED)
            );
            return;
        }

        try {
            database.setEventPriority(eventKey, priority);
            source.sendMessage(
                    Component.text("Set priority " + priority + " for event '" + eventKey + "'.")
            );
        } catch (EventNotFoundException ex) {
            source.sendMessage(
                    Component.text("No event found with key '" + eventKey + "'.")
                            .color(NamedTextColor.RED)
            );
        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while setting priority: " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }
}
