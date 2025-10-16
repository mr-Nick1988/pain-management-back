package pain_helper_back.common.patients.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
public class EmrUpdateDTO {
    private Double weight;
    private Double height;
    private String gfr;
    private String childPughScore;
    private Double plt;
    private Double wbc;
    private Double sat;
    private Double sodium;
    private List<String> sensitivities;
    private Set<DiagnosisDTO> diagnoses;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
