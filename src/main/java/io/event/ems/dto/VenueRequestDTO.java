package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VenueRequestDTO {

    @NotBlank(message = "Venue name cannot be blank")
    @Size(min = 3, max = 255, message = "Venue name must be between 3 and 255 characters")
    private String name;

    private String address;
    private String city;
    private String country;
}
