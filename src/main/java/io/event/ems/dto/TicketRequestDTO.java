package io.event.ems.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketRequestDTO {

    @NotBlank(message = "Name cannot be blank")
    private String name;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 12, fraction = 2, message = "Price must have up to 10 digits and 2 decimal places")
    private BigDecimal price;

    @Size(max = 1000, message = "Description must be less than 1000 characters")
    private String description;

    @NotNull(message = "Applies to section Id cannot be null")
    private UUID appliesToSectionId;

    private Integer totalQuantity;

    @FutureOrPresent(message = "Sale start date must be in the present or future")
    private LocalDateTime saleStartDate;

    @Future(message = "Sale end date must be in the future")
    private LocalDateTime saleEndDate;

    @NotNull(message = "Status Id cannot be null")
    private Integer statusId;
}
