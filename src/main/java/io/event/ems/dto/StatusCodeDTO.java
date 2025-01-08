package io.event.ems.dto;

import io.event.ems.validation.annotation.Uppercase;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatusCodeDTO {

    private Integer id;

    @NotBlank(message = "Entity type must not be blank")
    @Size(min = 3, max = 255, message = "Entity must be between 3 and 255 characters")
    @Uppercase
    private String entityType;

    @NotBlank(message = "Status must not be blank")
    @Size(min = 3, max = 255, message = "Status must be between 3 and 255 characters")
    @Uppercase
    private String status;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

}
