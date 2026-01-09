package pain_helper_back.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PersonDTO {
    private Long id;
    private String personId;
    private String firstName;
    private String lastName;
    private String login;
    private String role;
    private boolean temporaryCredentials;
}
