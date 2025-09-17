package pain_helper_back.common.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeCredentialsDTO {
    @NotBlank(message = "Login is required")
    private String login;
    @NotBlank(message = "Old password is required")
    private String oldPassword;
    @NotBlank(message = "New password is required")
    private String newPassword;
}
