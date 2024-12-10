package io.event.ems.dto;

import java.util.UUID;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserRequestDTO {

    private UUID id;

    @NotBlank(message = "Username cannot be blank")
    @Size(min = 5, max = 50, message = "User must be between 5 and 50 characters")
    private String username;

    @NotBlank(message = "Email cannot be blank")
    @Email(message = "Invalid email format")
    private String email;

    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    private String password;

    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;

    @Size(max = 20, message = "Phone number cannot exceed 20 characters")
    private String phone;

    private String role;

    private String avatarUrl;

    private Boolean emailVerified = false;
    private Boolean twoFactorEnabled = false;


}
