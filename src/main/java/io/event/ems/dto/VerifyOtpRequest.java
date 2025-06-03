package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {
    @NotBlank(message = "Identifier is required")
    private String identifier;

    @NotBlank(message = "OTP code is required")
    @Size(min = 6, max = 6, message = "OTP code must be exactly 6 characters")
    private String otp;
}
