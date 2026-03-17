package org.IFBX.isekaiGateway.exceptions;

public class EventAlreadyExistsException extends Exception {
    // handle key already exists failure
    public EventAlreadyExistsException(String eventKey) {
        super("Event already exists with key: " + eventKey);
    }
}