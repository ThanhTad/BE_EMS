package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendOtpRequest(
                @NotBlank String username,
                @NotBlank String otpType,
                String challengeToken) {

}
