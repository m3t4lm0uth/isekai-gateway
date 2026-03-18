package org.IFBX.isekaiGateway.exceptions;

// handle generic db failures
public class GatewayDatabaseException extends Exception {
    public GatewayDatabaseException(String message, Throwable cause) {
        super(message, cause);
    }
}