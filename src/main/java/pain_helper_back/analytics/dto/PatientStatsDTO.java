package pain_helper_back.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PatientStatsDTO {
    private Long totalPatients;
    private Map<String, Long> patientsByGender;
    private Map<String, Long> patientsByAgeGroup;
    private Long totalVasRecords;
    private Long criticalVasRecords;
    private Double averageVasLevel;
}

