package org.IFBX.isekaiGateway.api.networking;

// common supertype for all igw protocol messages
public sealed interface GoddessMessage
    permits GoddessPayloadCodec.PlayerMessage, GoddessPayloadCodec.SyncMessage
{
    byte opCode();
}
