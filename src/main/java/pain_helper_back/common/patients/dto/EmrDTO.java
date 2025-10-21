package pain_helper_back.common.patients.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class EmrDTO {
    @NotNull(message = "Height is required")
    @Range(min = 100, max = 250, message = "Height must be between 100cm and 250cm")
    private Double height;
    @NotNull(message = "Weight is required")
    @Range(min = 10, max = 330, message = "Weight must be between 10kg and 330kg")
    private Double weight;

    @Pattern(
            regexp = "^(?:[A-F]|(?:120|1[01]\\d|\\d{1,2}))$",
            message = "GFR must be either a letter (A-F) or an integer between 0(ml/min) and 120(ml/min)"
    )
    @NotNull(message = "GFR is required")
    private String gfr; //(функция почек)

    @Pattern(regexp = "^[A-C]$", message = "Child pugh score must be either A, B, or C")
    private String childPughScore;//(печень)

    @Range(min = 0, max = 1000, message = "PLT must be between 0(K/µL) and 1000(K/µL)")
    @NotNull(message = "PLT is required")
    private Double plt;//(тромбоциты)

    @DecimalMin(value = "2.0", message = "WBC must be at least 2.0(10³/µL)")
    @DecimalMax(value = "40.0", message = "WBC must be at most 40.0(10³/µL)")
    @NotNull(message = "WBC is required")
    private Double wbc;//(лейкоциты)

    @Range(min = 85, max = 100, message = "SAT must be between 85% and 100%")
    @NotNull(message = "SAT is required")
    private Double sat;//(сатурация)

    @Range(min = 120, max = 160, message = "Sodium must be between 120mEq/L and 160mEq/L")
    @NotNull(message = "Sodium is required")
    private Double sodium;//(натрий)

    private List<String> sensitivities;
    @Valid
    private Set<DiagnosisDTO> diagnoses;
    private LocalDateTime createdAt;
    private String createdBy;

    // пока опциональное поле, не используется на фронте
    private String patientMrn; // пригодится для поисковых запросов без обёртки Patient,чтоб понять к кому относится
    // создать нужный метод в сервисе и не забыть присвоить это поле (поиск всех EMR с gfr<60)
    // emrDto.setPatientMrn(emr.getPatient().getMrn());
}