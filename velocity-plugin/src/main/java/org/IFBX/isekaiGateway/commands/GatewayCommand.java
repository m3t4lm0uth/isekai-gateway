package org.IFBX.isekaiGateway.commands;

import java.util.Arrays;

import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.ProxyServer;
import org.IFBX.isekaiGateway.GatewayDatabase;
import org.IFBX.isekaiGateway.GoddessMessenger;

// command delegator
public class GatewayCommand implements SimpleCommand {
    // ------- fields -------
    private final ProxyServer server;
    private final GatewayDatabase database;
    private final GoddessMessenger messages;
    private final FlagSubHandler flagHandler;
    private final EventSubHandler eventHandler;
    private final ListSubHandler listHandler;

    // constructor
    public GatewayCommand(ProxyServer server, GatewayDatabase database, GoddessMessenger messages) {
        this.server = server;
        this.database = database;
        this.messages = messages;

        this.flagHandler = new FlagSubHandler(server, database, messages);
        this.listHandler = new ListSubHandler(server, database, messages);
        this.eventHandler = new EventSubHandler(database, messages, listHandler);
    }

    // ------- main methods --------
    // execute: /gw is ran.
    @Override
    public void execute(Invocation invocation) {

        CommandSource source = invocation.source(); // who ran the command
        String[] args = invocation.arguments(); // array of strings following command

        // ======= arg handling =======
        // no args
        if (args.length == 0) {
            source.sendMessage(messages.rootUsage());
            return;
        }

        String subcommand = args [0];

        // ======= subcommands =======
        // list subcommand
        if (subcommand.equalsIgnoreCase("list")) {
            String[] listArgs = Arrays.copyOfRange(args, 1, args.length);
            listHandler.handleListSubcommand(source, listArgs);
            return;
        }

        // flag subcommand
        if (subcommand.equalsIgnoreCase("trigger") || subcommand.equalsIgnoreCase("clear")) {
            String[] flagArgs = Arrays.copyOfRange(args, 1, args.length);
            flagHandler.handleFlagSubcommand(source, subcommand, flagArgs);
            return;
        }

        // event subcommand
        if (subcommand.equalsIgnoreCase("event")) {
            String[] eventArgs = Arrays.copyOfRange(args, 1, args.length);
            eventHandler.handleEventSubcommand(source, eventArgs);
            return;
        }

        // unknown subcommand
        source.sendMessage(messages.rootUsage());
    }
}
