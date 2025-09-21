package pain_helper_back.anesthesiologist.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EscalationStatsDTO {
    private Long totalEscalations;
    private Long pendingEscalations;
    private Long inProgressEscalations;
    private Long resolvedEscalations;
    private Long highPriorityEscalations;
    private Long mediumPriorityEscalations;
    private Long lowPriorityEscalations;
}
