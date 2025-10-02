package pain_helper_back.emr_integration.entity;


import jakarta.persistence.*;
import lombok.Data;
import pain_helper_back.enums.EmrSourceType;

import java.time.LocalDateTime;
@Entity
@Table(name = "emr_mappings")
@Data
public class EmrMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String externalFhirId;  // ID из FHIR системы

    @Column(unique = true, nullable = false)
    private String internalEmrNumber;  // Наш внутренний номер

    @Enumerated(EnumType.STRING)
    private EmrSourceType sourceType;

    private String sourceSystemUrl;
    private LocalDateTime importedAt;
    private String importedBy;

    @PrePersist
    public void prePersist() {
        importedAt = LocalDateTime.now();
    }
}

