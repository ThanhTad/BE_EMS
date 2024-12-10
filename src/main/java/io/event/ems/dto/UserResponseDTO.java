package io.event.ems.dto;

import java.time.LocalDateTime;
import java.util.UUID;

import io.event.ems.model.Role;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserResponseDTO {

    private UUID id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Role role;
    private String avatarUrl;
    private LocalDateTime createdAt;
    private Boolean emailVerified;
    private Boolean twoFactorEnabled;

}
