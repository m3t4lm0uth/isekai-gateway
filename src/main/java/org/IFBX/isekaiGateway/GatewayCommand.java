package org.IFBX.isekaiGateway;

// packages for velocity command interfaces
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
// velocity types
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.ProxyServer;
// packages for building the message(s)
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

// used for player detection
import java.util.Optional;

// class to build command for admins to manipulate player flags manually
public class GatewayCommand implements SimpleCommand {

    private final ProxyServer server;
    private final GatewayState state;
    private final GatewayMessenger messages;

    // constructor: allows other methods to use server/state
    public GatewayCommand(ProxyServer server, GatewayState state, GatewayMessenger messages) {
        this.server = server;
        this.state = state;
        this.messages = messages;
    }

    // execute: /gw is ran.
    @Override
    public void execute(Invocation invocation) {

        CommandSource source = invocation.source(); // who ran the command
        String[] args = invocation.arguments(); // array of strings following command

        // command should be called with 2 args
        if (args.length != 2) {
            source.sendMessage(Component.text("Usage: /isekaigateway <trigger|clear> <player>"));
            return;
        }

        String subcommand =  args[0];
        String targetName = args[1];

        // resolve target player
        Optional<Player> optionalPlayer = server.getPlayer(targetName);

        // stops execution if player not found
        if (optionalPlayer.isEmpty()) {
            source.sendMessage (Component.text("Player not found: " + targetName));
            return;
        }

        // set target to actual player
        Player target = optionalPlayer.get();

        if (subcommand.equalsIgnoreCase("trigger")){
            // flag as event-req'd in memory
            state.markEventRequired(target.getUniqueId());

            // disconnect message and notify source
            Component message = messages.buildEventTriggeredMessage();
            target.disconnect(message);
            source.sendMessage(Component.text("Triggered event disconnect for " + target.getUsername()));

        } else if (subcommand.equalsIgnoreCase("clear")) {
            //clear the flag
            state.clearEventRequired(target.getUniqueId());
            source.sendMessage(Component.text("Cleared event flag for " + target.getUsername()));
        } else {
            source.sendMessage (Component.text("Usage: /isekaigateway <trigger|clear> <player>"));
        }
    }
}
