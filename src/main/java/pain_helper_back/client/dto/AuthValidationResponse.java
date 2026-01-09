package pain_helper_back.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AuthValidationResponse {
    private boolean valid;
    private String personId;
    private String role;
    private String message;
}
