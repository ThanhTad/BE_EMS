package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class TwoFactorVerificationRequest extends VerifyOtpRequest {
    @NotBlank(message = "Challenge token is required")
    private String challengeToken;
}