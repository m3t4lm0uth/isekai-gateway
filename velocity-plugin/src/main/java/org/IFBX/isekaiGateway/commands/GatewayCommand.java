package org.IFBX.isekaiGateway.commands;

import org.IFBX.isekaiGateway.GatewayDatabase;
import org.IFBX.isekaiGateway.GatewayMessenger;
import org.IFBX.isekaiGateway.PlayerFlagFilter;
import org.IFBX.isekaiGateway.exceptions.EventAlreadyExistsException;
import org.IFBX.isekaiGateway.exceptions.EventNotFoundException;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.util.Arrays;
import java.util.Optional;
import java.util.UUID;

// build command for admins to manipulate player flags / events in-game
public class GatewayCommand implements SimpleCommand {

    // var init
    private final ProxyServer server;
    private final GatewayDatabase database;
    private final GatewayMessenger messages;

    // constructor: allows methods to use server/database
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
                Component.text("Usage: /isekaigateway <list | event | trigger | clear > ...")
        );
    }

    // flag args
    private void sendFlagUsage(CommandSource source, String subcommand) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway " + subcommand.toLowerCase() + " <player> <event_key>")
        );
    }

    // list root args
    private void sendListUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway list <actions | events | flags> ...")
        );
    }

    // list flags args
    private void sendListFlagsUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway list flags <all | active>")
        );
    }

    // event root args
    private void sendEventUsage(CommandSource source) {
        source.sendMessage(
                Component.text("Usage: /isekaigateway event <create | activate | deactivate | delete | map | rename | priority> ...")
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
                Component.text("Usage: /isekaigateway event map <backend | trigger> <event_key> <value>")
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

        // flag subcommand
        if (subcommand.equalsIgnoreCase("trigger") || subcommand.equalsIgnoreCase("clear")) {
            String[] flagArgs = Arrays.copyOfRange(args, 1, args.length);
            handleFlagSubcommand(source, subcommand, flagArgs);
            return;
        }

        // list subcommand
        if (subcommand.equalsIgnoreCase("list")) {
            String[] listArgs = Arrays.copyOfRange(args, 1, args.length);
            handleListSubcommand(source, listArgs);
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
            handleListEvents(source);
            return;
        }

        // else: command unknown
        sendEventUsage(source);
    }

    // ------- event modifications -------
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

    // map event backend | action
    private void handleEventMap(CommandSource source, String[] args) {
        if (args.length != 3) {
            sendMapUsage(source);
            return;
        }

        String action = args[0];
        String eventKey = args[1];
        String value = args[2];

        try {
            if (action.equalsIgnoreCase("backend")) {
                database.mapEventBackend(eventKey, value);
                source.sendMessage(
                        Component.text("Mapped event '" + eventKey + "' to backend '" + value + "'.")
                );
                return;
            } else if (action.equalsIgnoreCase("trigger")) {
                database.mapEventTrigger(eventKey, value);
                source.sendMessage(
                        Component.text("Mapped event '" + eventKey + "' to action '" + value + "'.")
                );
                return;
            }

            // unknown command
            sendMapUsage(source);

        } catch (EventNotFoundException ex) {
            source.sendMessage(
                    Component.text("No event found with key '" + eventKey + "'.")
                            .color(NamedTextColor.RED)
            );
        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while mapping " + action + ": " + ex.getMessage())
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

    // ------ lists -------
    // helper: handle when subcommand = list
    private void handleListSubcommand(CommandSource source, String[] args) {
        if (args.length < 1) {
            sendListUsage(source);
            return;
        }

        String target = args[0];

        // subcommand actions
        if (target.equalsIgnoreCase("actions")) {
            handleListActions(source);
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
        sendListUsage(source);
    }

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

    // list actions
    private void handleListActions(CommandSource source) {
        try {
            var actions = database.listActions();

            if (actions.isEmpty()) {
                source.sendMessage(
                        Component.text("No actions registered.")
                );
                return;
            }

            source.sendMessage(
                    Component.text("Actions:")
            );

            // build list display
            for (GatewayDatabase.ActionSummary a : actions) {
                // format display
                StringBuilder sb = new StringBuilder();
                sb.append('[').append(a.actionKey()).append(']');;
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
                    Component.text("Database error while listing actions: " + ex.getMessage())
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
                sendListFlagsUsage(source);
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
