package io.event.ems.dto;

import lombok.Data;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;

@Data
public class ApiResponse<T> {

    private int status;
    private String message;
    private T data;
    private LocalDateTime timestamp;

    public ApiResponse(HttpStatus status, String message, T data) {
        this.status = status.value();
        this.message = message;
        this.data = data;
        this.timestamp = LocalDateTime.now();
    }

    public ApiResponse(HttpStatus status, String message) {
        this.status = status.value();
        this.message = message;
        this.timestamp = LocalDateTime.now();
    }
    
    public static <T> ApiResponse<T> success(String message, T data){
        return new ApiResponse<>(HttpStatus.OK, message, data);
    }

    public static <T> ApiResponse<T> success(T data){
        return new ApiResponse<>(HttpStatus.OK, "Success", data);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message){
        return new ApiResponse<>(status, message, null);
    }

    public static <T> ApiResponse<T> error(HttpStatus status, String message, T data){
        return new ApiResponse<>(status, message, data);
    }

    public static <T> ApiResponse<T> created(T data){
        return new ApiResponse<>(HttpStatus.CREATED,"Created", data);
    }

}