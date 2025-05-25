package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TokenResponse {

    private Long accessTokenExpiresIn;
    private boolean twoFactorEnabled;
    private UserResponseDTO user;

}
