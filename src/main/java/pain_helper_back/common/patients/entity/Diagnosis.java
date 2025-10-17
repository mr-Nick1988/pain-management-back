package pain_helper_back.common.patients.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "diagnoses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Diagnosis {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // связь с EMR
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "emr_id")
    private Emr emr;

    //  код и описание болезни
    private String icdCode;        // E11.9
    private String description; // Type 2 diabetes mellitus
}