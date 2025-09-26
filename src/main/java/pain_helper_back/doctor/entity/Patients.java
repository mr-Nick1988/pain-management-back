package pain_helper_back.doctor.entity;

import jakarta.persistence.*;
import lombok.Data;
import pain_helper_back.admin.entity.Person;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "doctor_patients")
@Data
public class Patients {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "mrn", nullable = true, unique = true)
    private String mrn;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Column(nullable = false)
    private String gender;

    @Column(name = "insurance_policy_number")
    private String insurancePolicyNumber;

    @Column
    private String phoneNumber;

    @Column
    private String email;

    @Column(length = 500)
    private String address;

    @Column(length = 1000)
    private String additionalInfo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private Person createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private Person updatedBy;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}