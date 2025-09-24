package pain_helper_back.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PersonRegisterRequestDTO {
    @NotBlank(message = "Document ID is required")
    @Size(max = 20, message = "Document ID must not exceed 20 characters")
    private String personId;

    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|DOCTOR|NURSE|ANESTHESIOLOGIST", message = "Invalid role")
    private String role;

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Login is required")
    @Size(min = 3, max = 50, message = "Login must be between 3 and 50 characters")
    private String login;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be between 6 and 100 characters")
    private String password;
}
