package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CategoryRequestDTO {

    @NotBlank(message = "Category name cannot be blank")
    @Size(min = 2, max = 255, message = "Category name must be between 2 and 255 characters")
    private String name;

    private String description;
}
