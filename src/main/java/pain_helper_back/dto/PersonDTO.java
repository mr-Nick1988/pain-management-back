package pain_helper_back.dto;

import lombok.Data;

@Data
public class PersonDTO {
    private Long id;
    private String personId;
    private String firstName;
    private String lastName;
    private String login;
    private String role;
    private boolean temporaryCredentials;
}
