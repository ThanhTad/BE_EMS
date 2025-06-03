package io.event.ems.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Valid
public class ResetPasswordWithTokenRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String resetToken;

    @NotBlank
    @Size(min = 8, max = 64)
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,64}$", message = "Password must be between 8 and 64 characters long, contain at least one uppercase letter, one lowercase letter, one digit, and one special character.")
    private String newPassword;

    @NotBlank
    private String confirmPassword;

    @AssertTrue(message = "Passwords do not match")
    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);

    }
}
