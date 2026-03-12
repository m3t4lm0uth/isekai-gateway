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

// class to build command for admins to manually flag player
public class GatewayCommand implements SimpleCommand {

    private final ProxyServer server;
    private final GatewayState state;

    // constructor: allows other methods to use server
    public GatewayCommand(ProxyServer server, GatewayState state) {
        this.server = server;
        this.state = state;
    }

    // execute: method req'd by SimpleCommand; called when /gw is ran.
    @Override
    public void execute(Invocation invocation) {
        // who ran the command
        CommandSource source = invocation.source();
        // array of strings following command
        String[] args = invocation.arguments();

        // arg validation
        if (args.length != 2 || !args[0].equalsIgnoreCase("trigger")) {
            source.sendMessage (Component.text("Usage: /isekaigateway trigger <player>"));
            return;
        }

        // resolve target player
        String targetName = args[1];
        Optional<Player> optionalPlayer = server.getPlayer(targetName);

        // stops execution if player not found
        if (optionalPlayer.isEmpty()) {
            source.sendMessage (Component.text("Player not found: " + targetName));
            return;
        }

        // set target to actual player and flag as event-req'd in memory
        Player target = optionalPlayer.get();
        state.markEventRequired(target.getUniqueId());

        // build disconnect message
        Component message = Component.text()
                .append(Component.text("Event Pack Required\n").color(NamedTextColor.RED))
                .append(Component.text("See discord for current pack.").color(NamedTextColor.AQUA))
                .build();

        // disconnect message and notify source
        target.disconnect(message);
        source.sendMessage(Component.text("Triggered event disconnect for " + target.getUsername()));
    }
}
