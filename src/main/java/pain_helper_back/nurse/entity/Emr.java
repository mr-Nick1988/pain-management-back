package pain_helper_back.nurse.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;


@Entity
@Table(name = "emr")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Emr {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emr_id")
    private Long id;
    @Column(name = "glomerular_filtration_rate")
    private String gfr;
    @Column(name = "child_pugh_score")
    private String childPughScore;
    @Column(name = "platelets_count")
    private Double plt;
    @Column(name = "white_blood_cells")
    private Double wbc;
    @Column(name = "oxygen_saturation")
    private Double sat;
    @Column(name = "sodium_level")
    private Double sodium;
    @CreationTimestamp
    @Column(name = "created_at")
    private LocalDate timestamp;
    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;
}
