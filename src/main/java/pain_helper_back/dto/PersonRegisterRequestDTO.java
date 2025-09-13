package pain_helper_back.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PersonRegisterRequestDTO {
    @NotBlank(message = "Id is required")
    private String id;
    @NotBlank(message = "Role is required")
    private String role;
    @NotBlank(message = "First name is required")
    private String firstName;
    @NotBlank(message = "Last name is required")
    private String lastName;
    @NotBlank(message = "Login is required")
    @Email(message = "Login should be valid")
    private String login;
    @NotBlank(message = "Password is required")
    private String password;

}
