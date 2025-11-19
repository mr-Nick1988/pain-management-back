package pain_helper_back.reporting.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_report_aggregate")
@Getter
@Setter
public class DailyReportAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "report_date", nullable = false, unique = true)
    private LocalDate reportDate;

    @Column(name = "total_patients_registered")
    private Long totalPatientsRegistered;

    @Column(name = "total_vas_records")
    private Long totalVasRecords;

    @Column(name = "average_vas_level")
    private Double averageVasLevel;

    @Column(name = "critical_vas_count")
    private Long criticalVasCount;

    @Column(name = "total_recommendations")
    private Long totalRecommendations;

    @Column(name = "approved_recommendations")
    private Long approvedRecommendations;

    @Column(name = "rejected_recommendations")
    private Long rejectedRecommendations;

    @Column(name = "approval_rate")
    private Double approvalRate;

    @Column(name = "total_escalations")
    private Long totalEscalations;

    @Column(name = "total_logins")
    private Long totalLogins;

    @Column(name = "successful_logins")
    private Long successfulLogins;

    @Column(name = "failed_logins")
    private Long failedLogins;

    @Column(name = "unique_active_users")
    private Long uniqueActiveUsers;

    @Column(name = "top_drugs_json", columnDefinition = "TEXT")
    private String topDrugsJson;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.createdBy == null) {
            this.createdBy = "system";
        }
    }
}
