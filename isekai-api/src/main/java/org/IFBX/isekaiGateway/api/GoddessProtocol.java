package org.IFBX.isekaiGateway.api;

// define what the protocol is
public final class GoddessProtocol {
    // static-only utility class
    private GoddessProtocol() {}

    // ------ var init ------
    // protocol version for if/when wire format evolves
    public static final byte PROTOCOL_VERSION = 1;
    // init messaging channel
    public static final String CHANNEL_NAMESPACE = "isekai";
    public static final String CHANNEL_NAME = "gateway";
    public static final String CHANNEL_ID = CHANNEL_NAMESPACE + ":" + CHANNEL_NAME;

    public static final byte OP_TRIGGER = 0x01;
    public static final byte OP_CLEAR = 0x02;

}