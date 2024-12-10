package io.event.ems.exception;

public class StatusNotFoundException extends ResourceNotFoundException {
    public StatusNotFoundException(String message) {
        super(message);
    }
}
