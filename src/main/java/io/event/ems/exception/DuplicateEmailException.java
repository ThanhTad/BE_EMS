package io.event.ems.exception;

public class DuplicateEmailException extends RuntimeException {

    public DuplicateEmailException(String message){
        super(message);
    }

}
