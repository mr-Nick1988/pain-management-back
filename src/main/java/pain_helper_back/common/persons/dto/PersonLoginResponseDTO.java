package pain_helper_back.common.persons.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PersonLoginResponseDTO {
    private String firstName;
    private String role;
    private boolean temporaryCredentials;
}
