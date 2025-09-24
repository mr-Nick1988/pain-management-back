package pain_helper_back.nurse.entity;

import jakarta.persistence.*;
import lombok.Data;

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
    @Column(name = "interval")
    private String interval;
    @Column(name = "route")
    private String route;
    @Column(name = "age_adjustment")
    private String ageAdjustment;
    @Column(name = "weight_adjustment")
    private String weightAdjustment;
    @Column(name = "child_pugh")
    private String childPugh;
    @ManyToOne
    @JoinColumn(name = "recommendation_id")
    private Recommendation recommendation;
}
