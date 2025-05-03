package io.event.ems.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    private String identifier;

    @NotBlank
    @Size(min = 6, max = 6)
    private String otp;

    @NotBlank
    private String otpType;

}
