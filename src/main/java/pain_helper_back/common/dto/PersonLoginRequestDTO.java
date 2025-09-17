package pain_helper_back.common.dto;


import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PersonLoginRequestDTO {
    @NotBlank(message = "Login is required")
    private String login;
    @NotBlank(message = "Password is required")
    private String password;
}
