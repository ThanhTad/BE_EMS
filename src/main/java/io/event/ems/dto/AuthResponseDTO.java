package io.event.ems.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponseDTO {

    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private boolean mfaRequired = false;
    private String username;

    public AuthResponseDTO(String accessToken, String refreshToken, String username){
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.username = username;
    }

    public AuthResponseDTO(boolean mfaRequired, String username){
        this.mfaRequired = mfaRequired;
        this.username = username;
    }

}
