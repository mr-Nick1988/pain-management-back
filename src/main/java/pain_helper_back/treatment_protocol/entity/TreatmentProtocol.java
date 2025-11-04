package pain_helper_back.treatment_protocol.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Entity
@Table(name = "treatment_protocol")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
public class TreatmentProtocol {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String painLevel;
    private String regimenHierarchy;
    private String route;
    private String firstDrug;
    private String firstDrugActiveMoiety;
    private String firstDosingMg;
    private String firstAgeAdjustments;
    private String firstIntervalHrs;
    private String weightKg;
    private String firstChildPugh;

    private String secondDrugActiveMoiety;
    private String secondDosingMg;
    private String secondAgeAdjustments;
    private String secondIntervalHrs;
    private String secondWeightKg;
    private String secondChildPugh;

    private String gfr;
    private String plt;
    private String wbc;
    private String sat;
    private String sodium;

    private String avoidIfSensitivity;
    @Column(length = 1000)
    private String contraindications;

    // 23 fields

}