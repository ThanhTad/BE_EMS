package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;

public record Enable2FARequest(
        @NotBlank String username) {
}
