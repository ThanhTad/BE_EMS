package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record Enable2FARequest(
                @NotBlank(message = "Username is required") String username,

                @NotBlank(message = "OTP is required") @Size(min = 6, max = 6, message = "OTP must be 6 digits") String otp) {
}
