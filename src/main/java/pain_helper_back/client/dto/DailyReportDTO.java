package pain_helper_back.client.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for daily report data from Reporting Service
 */
@Getter
@Setter
public class DailyReportDTO {
    private Long id;
    private LocalDate reportDate;
    
    // Patient statistics
    private Long totalPatientsRegistered;
    private Long totalVasRecords;
    private Double averageVasLevel;
    private Long criticalVasCount;
    
    // Recommendation statistics
    private Long totalRecommendations;
    private Long approvedRecommendations;
    private Long rejectedRecommendations;
    private Double approvalRate;
    
    // Escalation statistics
    private Long totalEscalations;
    private Long resolvedEscalations;
    private Long pendingEscalations;
    private Double averageResolutionTimeHours;
    
    // Performance statistics
    private Double averageProcessingTimeMs;
    private Long totalOperations;
    private Long failedOperations;
    
    // User activity
    private Long totalLogins;
    private Long uniqueActiveUsers;
    private Long failedLoginAttempts;
    
    // Metadata
    private LocalDateTime createdAt;
    private String createdBy;
    private Long sourceEventsCount;
}
