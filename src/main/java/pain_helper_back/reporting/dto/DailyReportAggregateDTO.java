package pain_helper_back.reporting.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
public class DailyReportAggregateDTO {

    private Long id;
    private LocalDate reportDate;
    private Long totalPatientsRegistered;
    private Long totalVasRecords;
    private Double averageVasLevel;
    private Long criticalVasCount;
    private Long totalRecommendations;
    private Long approvedRecommendations;
    private Long rejectedRecommendations;
    private Double approvalRate;
    private Long totalEscalations;
    private Long totalLogins;
    private Long successfulLogins;
    private Long failedLogins;
    private Long uniqueActiveUsers;
    private String topDrugsJson;
    private LocalDateTime createdAt;
    private String createdBy;
}
