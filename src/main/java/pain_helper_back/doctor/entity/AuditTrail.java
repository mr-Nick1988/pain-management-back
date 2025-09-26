package pain_helper_back.doctor.entity;

import jakarta.persistence.*;
import lombok.Data;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.enums.PatientRegistrationAuditAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_trails")
@Data
//Fixation audit and all actions of medical staff
public class AuditTrail {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private PatientRegistrationAuditAction action;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private Person person;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "pid", nullable = false)
    private Long pid; //Patient ID for the medical system, not the NATIONAL ID

    @Column(length = 1000)
    private String details;

    @PrePersist
    protected void onCreate() {
        timestamp = LocalDateTime.now();
    }
}
