package io.event.ems.exception;

public class InvalidPasswordException extends RuntimeException {

    public InvalidPasswordException(String message){
        super(message);
    }

}
