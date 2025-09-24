package pain_helper_back.anesthesiologist.entity;


import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "escalations")
@NoArgsConstructor
@AllArgsConstructor
public class Escalation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String patientId;
    @Column(nullable = false)
    private String patientName;
    @Column(nullable = false)
    private String doctorName;
    @Column(nullable = false,length = 1000)
    private String rejectedReason;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalationPriority escalationPriority;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EscalationStatus escalationStatus;
    @Column
    private String resolution;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private LocalDateTime resolvedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if(escalationStatus == null){
            escalationStatus = EscalationStatus.PENDING;
        }
        if(escalationPriority == null){
            escalationPriority = EscalationPriority.LOW;
        }
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
