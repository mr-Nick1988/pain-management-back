package pain_helper_back.common.patients.entity;

import jakarta.persistence.*;
import lombok.Data;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.enums.DrugRoute;

@Entity
@Data
@Table(name = "drug_recommendations")
public class DrugRecommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "drug_id")
    private Long id;

    @Column(name = "drug_name")
    private String drugName;
    @Column(name = "active_moiety")
    private String activeMoiety;
    @Column(name = "dosing")
    private String dosing;
    @Column(name = "dosage_interval")
    private String interval;
    @Enumerated(EnumType.STRING)
    @Column(name = "route")
    private DrugRoute route;
    @Column(name = "age_adjustment")
    private String ageAdjustment;
    @Column(name = "weight_adjustment")
    private String weightAdjustment;
    @Column(name = "child_pugh")
    private String childPugh;
    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    private DrugRole role;                // основное лекарство или альтернативное
    @ManyToOne
    @JoinColumn(name = "recommendation_id")
    private Recommendation recommendation;
}
