package io.event.ems.exception;

import java.util.Date;
import java.util.List;

import lombok.Data;

@Data
public class ErrorResponse {

    private int status;
    private String message;
    private Date timestamp;
    private String path;
    private List<String> errors;

    public ErrorResponse(int status, String message, String path){
        this.status = status;
        this.message = message;
        this.timestamp = new Date();
        this.path = path;
    }

      public ErrorResponse(int status, String message, String path, List<String> errors) {
        this.status = status;
        this.message = message;
        this.timestamp = new Date();
        this.path = path;
        this.errors = errors;
    }

}
