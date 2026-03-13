package org.IFBX.isekaiGateway.exceptions;

public class GatewayDatabaseException extends Exception {
    // handle generic db failures
    public GatewayDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}