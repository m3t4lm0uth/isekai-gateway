package org.IFBX.isekaiGateway.exceptions;

// handle no matches for key failure
public class EventNotFoundException extends Exception {
    public EventNotFoundException(String eventKey) {
        super("Event not found with key: " + eventKey);
    }
}