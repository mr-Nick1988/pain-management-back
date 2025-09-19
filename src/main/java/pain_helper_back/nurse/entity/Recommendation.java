package pain_helper_back.nurse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.util.List;

@Entity
@Data
@Table(name = "recommendation")
public class Recommendation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long id;
    @Column(name = "regimen_hierarchy")
    private int regimenHierarchy;
    @OneToMany(mappedBy = "recommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DrugRecommendation> drugs;
    @ElementCollection //Эта аннотация используется для хранения простых коллекций (recommendation_id,element)
    @CollectionTable(name = "recommendation_avoid_sensitivity", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "element")
    private List<String> avoidIfSensitivity;
    @ElementCollection
    @CollectionTable(name = "recommendation_contraindications", joinColumns = @JoinColumn(name = "recommendation_id"))
    @Column(name = "element")
    private List<String> contraindications;
    private String status;
    private String notes;
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDate timestamp;
    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;
}
