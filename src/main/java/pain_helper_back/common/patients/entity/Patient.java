package pain_helper_back.common.patients.entity;
import jakarta.persistence.*;
import lombok.*;
import pain_helper_back.enums.PatientsGenders;
import pain_helper_back.pain_escalation_tracking.entity.DoseAdministration;
import pain_helper_back.pain_escalation_tracking.entity.PainEscalation;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@Setter
@RequiredArgsConstructor
@EqualsAndHashCode(of = "mrn")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long id;
    @Column(name = "mrn", unique = true)
    private String mrn;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "gender")
    private PatientsGenders gender;
    @Column(name = "insurance_policy_number")
    private String insurancePolicyNumber;
    @Column(name = "phone_number")
    private String phoneNumber;
    @Column(name = "email")
    private String email;
    @Column(name = "address")
    private String address;
    @Column(name = "additional_info")
    private String additionalInfo;


    @Column(name = "created_by",updatable = false)
    private String createdBy;
    @Column(name="updated_by")
    private String updatedBy;
    private Boolean isActive;
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // Устанавливаем createdBy только если не задан явно
        if (this.createdBy == null) {
            this.createdBy = "system";
        }
    }
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
        // Устанавливаем updatedBy только если не задан явно
        if (this.updatedBy == null) {
            this.updatedBy = "system";
        }
    }

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Emr> emr;
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Vas> vas;
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<Recommendation> recommendations;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PainEscalation> painEscalations = new ArrayList<>();
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<DoseAdministration> doseAdministrations = new ArrayList<>();

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }

}
