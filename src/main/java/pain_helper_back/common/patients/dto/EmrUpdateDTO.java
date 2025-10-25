package pain_helper_back.common.patients.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.hibernate.validator.constraints.Range;
import org.springframework.lang.Nullable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class EmrUpdateDTO {
    @Range(min = 10, max = 330, message = "Weight must be between 10kg and 330kg")
    private Double weight;
    @Range(min = 100, max = 250, message = "Height must be between 100cm and 250cm")
    private Double height;
    @Pattern(
            regexp = "^(?:[A-F]|(?:120|1[01]\\d|\\d{1,2}))$",
            message = "GFR must be either a letter (A-F) or an integer between 0(ml/min) and 120(ml/min)"
    )
    private String gfr;
    @Pattern(regexp = "^$|^[A-C]$", message = "Child Pugh score must be either A, B, or C")
    @Nullable
    private String childPughScore;
    @Range(min = 0, max = 1000, message = "PLT must be between 0(K/µL) and 1000(K/µL)")
    private Double plt;
    @DecimalMin(value = "2.0", message = "WBC must be at least 2.0(10³/µL)")
    @DecimalMax(value = "40.0", message = "WBC must be at most 40.0(10³/µL)")
    private Double wbc;
    @Range(min = 85, max = 100, message = "SAT must be between 85% and 100%")
    private Double sat;
    @Range(min = 120, max = 160, message = "Sodium must be between 120mEq/L and 160mEq/L")
    private Double sodium;
    private List<String> sensitivities;
    @Valid
    private Set<DiagnosisDTO> diagnoses;
    private LocalDateTime updatedAt;
    private String updatedBy;
}