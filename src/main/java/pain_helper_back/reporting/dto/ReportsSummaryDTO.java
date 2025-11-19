package pain_helper_back.reporting.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class ReportsSummaryDTO {
    private PeriodStats period;
    private PatientStats patients;
    private RecommendationStats recommendations;
    private EscalationStats escalations;
    private UserStats users;

    @Data
    public static class PeriodStats {
        private LocalDate startDate;
        private LocalDate endDate;
        private Integer daysCount;
    }

    @Data
    public static class PatientStats {
        private Long totalRegistered;
        private Long totalVasRecords;
        private Double averageVasLevel;
    }

    @Data
    public static class RecommendationStats {
        private Long total;
        private Long approved;
        private Long rejected;
        private Double approvalRate;
    }

    @Data
    public static class EscalationStats {
        private Long total;
    }

    @Data
    public static class UserStats {
        private Long totalLogins;
        private Long successfulLogins;
        private Long failedLogins;
    }
}
