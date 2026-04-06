package org.IFBX.isekaiGateway.commands;

import java.util.Arrays;

import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.IFBX.isekaiGateway.GatewayDatabase;
import org.IFBX.isekaiGateway.GoddessMessenger;
import org.IFBX.isekaiGateway.exceptions.EventAlreadyExistsException;
import org.IFBX.isekaiGateway.exceptions.EventNotFoundException;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

// commands for admins to manipulate events in-game
public class EventSubHandler {
    // ------- fields -------
    private final GatewayDatabase database;
    private final GoddessMessenger messages;
    private final ListSubHandler listHandler;

    // constructor
    public EventSubHandler(GatewayDatabase database, GoddessMessenger messages, ListSubHandler listHandler) {
        this.database = database;
        this.messages = messages;
        this.listHandler = listHandler;
    }

    // ------- subcommand handler -------
    public void handleEventSubcommand(CommandSource source, String[] args) {
        if (args.length < 1) {
            source.sendMessage(messages.eventUsage());
            return;
        }

        String subcommand = args[0];

        // subcommand create
        if (subcommand.equalsIgnoreCase("create")) {
            String[] createArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventCreate(source, createArgs);
            return;
        }

        // subcommand activate | deactivate
        if (subcommand.equalsIgnoreCase("activate") || subcommand.equalsIgnoreCase("deactivate")) {
            handleEventStatusUpdate(source, args);
            return;
        }

        // subcommand delete
        if (subcommand.equalsIgnoreCase("delete")) {
            String[] deleteArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventDelete(source, deleteArgs);
            return;
        }

        //subcommand map
        if (subcommand.equalsIgnoreCase("map")) {
            String[] mapArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventMap(source, mapArgs);
            return;
        }

        //subcommand rename
        if (subcommand.equalsIgnoreCase("rename")) {
            String[] renameArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventRename(source, renameArgs);
            return;
        }

        //subcommand priority
        if (subcommand.equalsIgnoreCase("priority")) {
            String[] priorityArgs = Arrays.copyOfRange(args, 1, args.length);
            handleEventPriority(source, priorityArgs);
            return;
        }

        // subcommand list
        if (subcommand.equalsIgnoreCase("list")) {
            listHandler.handleListSubcommand(source, new String[] { "events" });
            return;
        }

        // else: command unknown
        source.sendMessage(messages.eventUsage());
    }

    // ------ subcommand methods -------
    // create event -args: <event_key> <name...>
    private void handleEventCreate(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(messages.eventSubUsage("create"));
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
            source.sendMessage(messages.eventSubUsage("activate"));
            return;
        }

        String trigger = args[0];
        String eventKey = args[1];

        try {
            if (trigger.equalsIgnoreCase("activate")) {
                database.activateEvent(eventKey);
                source.sendMessage(
                        Component.text("Activated event '" + eventKey + "'.")
                );
                return;
            } else if (trigger.equalsIgnoreCase("deactivate")) {
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
            source.sendMessage(messages.eventSubUsage("delete"));
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

    // map event backend | trigger
    private void handleEventMap(CommandSource source, String[] args) {
        if (args.length != 3) {
            source.sendMessage(messages.eventSubUsage("map"));
            return;
        }

        String subcommand = args[0];
        String eventKey = args[1];
        String value = args[2];

        try {
            if (subcommand.equalsIgnoreCase("backend")) {
                database.mapEventBackend(eventKey, value);
                source.sendMessage(
                        Component.text("Mapped event '" + eventKey + "' to backend '" + value + "'.")
                );
                return;
            } else if (subcommand.equalsIgnoreCase("trigger")) {
                database.mapEventTrigger(eventKey, value);
                source.sendMessage(
                        Component.text("Mapped event '" + eventKey + "' to trigger '" + value + "'.")
                );
                return;
            }

            // unknown command
            source.sendMessage(messages.eventSubUsage("map"));

        } catch (EventNotFoundException ex) {
            source.sendMessage(
                    Component.text("No event found with key '" + eventKey + "'.")
                            .color(NamedTextColor.RED)
            );
        } catch (GatewayDatabaseException ex) {
            source.sendMessage(
                    Component.text("Database error while mapping " + subcommand + ": " + ex.getMessage())
                            .color(NamedTextColor.RED)
            );
        }
    }

    // rename event
    private void handleEventRename(CommandSource source, String[] args) {
        if (args.length < 2) {
            source.sendMessage(messages.eventSubUsage("rename"));
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
            source.sendMessage(messages.eventSubUsage("priority"));
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

    // ------- helpers -------
    // support for multi-word names
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
}
