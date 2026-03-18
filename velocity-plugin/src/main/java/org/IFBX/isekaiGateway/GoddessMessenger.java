package org.IFBX.isekaiGateway;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

// messages for the user
public class GoddessMessenger {

    // ------- gameplay messages -------
    // disconnect screen when event triggered
    public Component buildEventTriggeredMessage() {
        return Component.text()
                .append(Component.text("Event Pack Required\n").color(NamedTextColor.RED))
                .append(Component.text("See discord for current pack.").color(NamedTextColor.AQUA))
                .build();
    }
    
    // ------- command arg messages -------
    // ======= root usage =======
    // root args
    public Component rootUsage() {
        return Component.text("Usage: /isekaigateway <list | event | trigger | clear > ...");
    }

    // ======= list-related usage =======
    // generic list args
    public Component listUsage() {
        return Component.text("Usage: /isekaigateway list <actions | events | flags> ...");
    }

    // list flags args
    public Component listFlagsUsage() {
        return Component.text("Usage: /isekaigateway list flags <all | active>");
    }

    // ======= flag-related usage =======
    // trigger | clear flag args
    public Component flagUsage(String action) {
        return Component.text("Usage: /isekaigateway " + action.toLowerCase() + " <player> <event_key>");
    }

    // ======= event-related usage =======
    // generic event args
    public Component eventUsage() {
        return Component.text("Usage: /isekaigateway event <create | activate | deactivate | delete | map | rename | priority> ...");
    }
    
    // event sub-usage: variant chosen with action keyword
    public Component eventSubUsage(String action) {
        return switch (action.toLowerCase()) {
            case "create" -> Component.text("Usage: /isekaigateway event create <event_key> <name>");
            case "activate", "deactivate" -> Component.text("Usage: /isekaigateway event <activate|deactivate> <event_key>");
            case "delete" -> Component.text("Usage: /isekaigateway event delete <event_key>");
            case "map" -> Component.text("Usage: /isekaigateway event map <backend | trigger> <event_key> <value>");
            case "rename" -> Component.text("Usage: /isekaigateway event rename <event_key> <new_name>");
            case "priority" -> Component.text("Usage: /isekaigateway event priority <event_key> <priority>");
            default -> eventUsage();
        };
    }
}