package org.IFBX.isekaiGateway;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

public class GatewayMessenger {

    public Component buildEventTriggeredMessage() {
        // build disconnect message
        return Component.text()
                .append(Component.text("Event Pack Required\n").color(NamedTextColor.RED))
                .append(Component.text("See discord for current pack.").color(NamedTextColor.AQUA))
                .build();
    }
}