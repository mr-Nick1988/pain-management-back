package pain_helper_back.client.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserInfoResponse {
    private String personId;
    private String firstName;
    private String lastName;
    private String login;
    private String role;
    private boolean temporaryCredentials;
}
