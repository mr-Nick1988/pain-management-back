package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class EmrDTO {
    @NotNull(message = "Height is required")
    private Double height;
    @NotNull(message = "Weight is required")
    private Double weight;
    @NotNull(message = "GFR is required")
    private String gfr; //(функция почек)
    @NotNull(message = "Child pugh score is required")
    private String childPughScore;//(печень)
    @NotNull(message = "PLT is required")
    private Double plt;//(тромбоциты)
    @NotNull(message = "WBC is required")
    private Double wbc;//(лейкоциты)
    @NotNull(message = "SAT is required")
    private Double sat;//(сатурация)
    @NotNull(message = "Sodium is required")
    private Double sodium;//(натрий)
    private LocalDateTime createdAt;
    private String createdBy;
}
