package pain_helper_back.client.dto;

import lombok.Data;

@Data
public class UserInfoResponse {
    private String personId;
    private String firstName;
    private String lastName;
    private String login;
    private String role;
    private boolean temporaryCredentials;
}
