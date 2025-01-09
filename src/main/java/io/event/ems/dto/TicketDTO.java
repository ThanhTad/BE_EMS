package io.event.ems.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TicketDTO {

    private UUID id;

    @NotNull(message = "Event Id cannot be null")
    private UUID eventId;

    @NotBlank(message = "Ticket type cannot be blank")
    @Size(max = 255, message = "Ticket type must be less than 255 charaters")
    private String ticketType;

    @NotNull(message = "Price cannot be null")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    @Digits(integer = 10, fraction = 2, message = "Price must have up to 10 digits and 2 decimal places")
    private BigDecimal price;

    @NotNull(message = "Total quantity cannot be null")
    @Min(value = 1, message = "Total quantity must be at least 1")
    private Integer totalQuantity;

    private Integer availableQuantity;

    @FutureOrPresent(message = "Sale start date must be in the present or future")
    private LocalDateTime saleStartDate;

    @Future(message = "Sale end date must be in the future")
    private LocalDateTime saleEndDate;

    @NotNull(message = "Status Id cannot be null")    
    private Integer statusId;

    @Min(value = 1, message = "Max per user must be at least 1")
    @Max(value = 5, message = "Max per user must be at most 5")
    private Integer maxPerUser;

    @Size(max = 1000, message = "Description must be less than 1000 charaters")
    private String description;

    @DecimalMin(value = "0.0", message = "Early bird discount must be greater than or equal to 0")
    @DecimalMax(value = "1.0", message = "Early bird discount must be less than or equal to 1")
    private BigDecimal earlyBirdDiscount;

    private Boolean isFree;

}
