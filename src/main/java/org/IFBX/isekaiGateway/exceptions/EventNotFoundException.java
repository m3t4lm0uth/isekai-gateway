package org.IFBX.isekaiGateway.exceptions;

public class EventNotFoundException extends Exception {
    // handle no matches for key failure
    public EventNotFoundException(String eventKey) {
        super("Event not found with key: " + eventKey);
    }
}