package pain_helper_back.dto;

import lombok.Data;

@Data
public class PersonDto {
    private Long id;
    private String firstName;
    private String lastName;
    private String login;
    private String role;
    private boolean temporaryCredentials;
}
