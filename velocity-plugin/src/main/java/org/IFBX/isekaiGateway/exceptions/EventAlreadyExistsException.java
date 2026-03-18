package org.IFBX.isekaiGateway.exceptions;

// handle key already exists failure
public class EventAlreadyExistsException extends Exception {
    public EventAlreadyExistsException(String eventKey) {
        super("Event already exists with key: " + eventKey);
    }
}