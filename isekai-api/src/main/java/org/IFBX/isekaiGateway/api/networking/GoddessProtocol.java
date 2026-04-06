package org.IFBX.isekaiGateway.api.networking;

// static-only utility class; defines protocol
public final class GoddessProtocol {
    // ------ fields ------
    // protocol version for if/when wire format evolves
    public static final byte PROTOCOL_VERSION = 1;
    // messaging channel
    public static final String CHANNEL_NAMESPACE = "isekai";
    public static final String CHANNEL_NAME = "gateway";
    public static final String CHANNEL_ID = CHANNEL_NAMESPACE + ":" + CHANNEL_NAME;
    // operation codes
    public static final byte OP_SYNC_TRIGGERS = 0x01;
    public static final byte OP_TRIGGER = 0x02;
    public static final byte OP_CLEAR = 0x03;

    // constructor
    private GoddessProtocol() {}
}