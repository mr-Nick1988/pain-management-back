package pain_helper_back.anesthesiologist.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pain_helper_back.enums.EscalationPriority;

@Data
public class EscalationCreateDTO {
    @NotNull(message = "Recommendation ID is required")
    private Long recommendationId;
    @NotBlank(message = "Escalated by is required")
    @Size(max = 50, message = "Escalated by must be less than 50 characters")
    private String escalatedBy;//doctor id
    @NotNull(message = "Escalation reason is required")
    @Size(max = 100, message = "Escalation reason must be less than 100 characters")
    private String escalationReason;
    @NotNull(message = "Escalation priority is required")
    private EscalationPriority escalationPriority;
    @Size(max = 2000, message = "Description must not exceed 2000 characters")
    private String description;

}
