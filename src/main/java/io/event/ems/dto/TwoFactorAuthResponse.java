package io.event.ems.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TwoFactorAuthResponse {
    private boolean twoFactorEnabled;
    private String challengeToken;
    private String message;
}