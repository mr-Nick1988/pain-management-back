package pain_helper_back.anesthesiologist.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class EscalationResolutionDTO {


    @NotBlank(message = "Resolved by is required")
    @Size(max = 50, message = "Resolved by must not exceed 50 characters")
    private String resolvedBy;  // Anesthesiologist ID

    @NotBlank(message = "Resolution is required")
    @Size(max = 2000, message = "Resolution must not exceed 2000 characters")
    private String resolution;

    @Size(max = 1000, message = "Comment must not exceed 1000 characters")
    private String comment;  // Комментарий анестезиолога

    private Boolean approved;  // true = approve, false = reject
}
