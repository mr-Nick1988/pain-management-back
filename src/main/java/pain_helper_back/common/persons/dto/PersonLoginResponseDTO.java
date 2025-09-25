package pain_helper_back.common.persons.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonLoginResponseDTO {
    private String firstName;
    private String role;
    private boolean temporaryCredentials;
}
