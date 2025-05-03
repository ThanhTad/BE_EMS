package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;

public record Disable2FARequest(
        @NotBlank String username,
        @NotBlank String otp) {

}
