package pain_helper_back.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserActivityDTO {
    private String userId;
    private String userRole;
    private Long totalActions;
    private LocalDateTime lastActivity;
    private Long loginCount;
    private Long failedLoginCount;
}
