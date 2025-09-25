package pain_helper_back.common.persons.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ChangeCredentialsDTO {
    @NotBlank(message = "Current login is required")
    private String currentLogin;
    @NotBlank(message = "New login is required")
    private String newLogin;
    @NotBlank(message = "Old password is required")
    private String oldPassword;
    @NotBlank(message = "New password is required")
    private String newPassword;
}
