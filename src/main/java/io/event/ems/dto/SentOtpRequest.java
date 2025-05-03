package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;

public record SentOtpRequest(
        @NotBlank String username) {

}
