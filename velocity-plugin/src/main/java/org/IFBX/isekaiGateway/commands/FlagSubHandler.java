package org.IFBX.isekaiGateway.commands;

import java.util.Optional;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.IFBX.isekaiGateway.GatewayDatabase;
import org.IFBX.isekaiGateway.GoddessMessenger;
import org.IFBX.isekaiGateway.exceptions.EventNotFoundException;
import org.IFBX.isekaiGateway.exceptions.GatewayDatabaseException;

// commands for admins to manipulate player flags in-game
public class FlagSubHandler {
    // ------- fields -------
    private final ProxyServer server;
    private final GatewayDatabase database;
    private final GoddessMessenger messages;

    // constructor
    public FlagSubHandler(ProxyServer server, GatewayDatabase database, GoddessMessenger messages) {
        this.server = server;
        this.database = database;
        this.messages = messages;
    }

    // ------ subcommand handler -------
    public void handleFlagSubcommand(CommandSource source, String subcommand, String[] args) {
        // ======= arg handling =======
        // missing args
        if (args.length != 2) {
            source.sendMessage(messages.flagUsage(subcommand));
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

        // ======= subcommand methods =======
        // trigger event-req'd player flag
        if (subcommand.equalsIgnoreCase("trigger")){
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
        }

        // clear event-req'd player flag
        else if (subcommand.equalsIgnoreCase("clear")) {
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

        }

        // unknown subcommand
        else {
            source.sendMessage(messages.flagUsage(subcommand));
        }
    }
}
