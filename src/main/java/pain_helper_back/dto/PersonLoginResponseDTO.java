package pain_helper_back.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PersonLoginResponseDTO {
    private String token;
    private String role;
    private boolean isTemporaryCredentials;
}
