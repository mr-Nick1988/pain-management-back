package pain_helper_back.nurse.entity;

import jakarta.persistence.*;
import lombok.*;
import pain_helper_back.nurse.PatientsGenders;


import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@Entity
@Table(name = "patients")
@Getter
@Setter
@ToString
@RequiredArgsConstructor
@EqualsAndHashCode(of = "personId")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "patient_id")
    private Long id;
    @Column(name = "first_name")
    private String firstName;
    @Column(name = "last_name")
    private String lastName;
    @Column(name = "person_id", unique = true, nullable = false)
    private String personId;
    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;
    @Column(name = "gender")
    private PatientsGenders gender;
    @Column(name = "height")
    private Double height;
    @Column(name = "weight")
    private Double weight;

    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Emr> emr;
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Vas> vas;
    @OneToMany(mappedBy = "patient", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recommendation> recommendations;

    public int getAge() {
        return Period.between(dateOfBirth, LocalDate.now()).getYears();
    }
}
