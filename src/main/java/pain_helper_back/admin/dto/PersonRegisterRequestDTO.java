package pain_helper_back.admin.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PersonRegisterRequestDTO {
    @NotBlank(message = "Document ID is required")
    private String personId;
    
    @NotBlank(message = "Role is required")
    @Pattern(regexp = "ADMIN|DOCTOR|NURSE|ANESTHESIOLOGIST", message = "Invalid role")
    private String role;
    
    @NotBlank(message = "First name is required")
    private String firstName;
    
    @NotBlank(message = "Last name is required")
    private String lastName;
    
    @NotBlank(message = "Login is required")
    private String login;
    
    @NotBlank(message = "Password is required")
    private String password;
}
