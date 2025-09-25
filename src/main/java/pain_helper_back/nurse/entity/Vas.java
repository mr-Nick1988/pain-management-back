package pain_helper_back.nurse.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;

@Entity
@Data
@Table(name = "vas")

public class Vas {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vas_id")
    private Long id;
    @Column(name = "pain_place")
    private String painPlace;
    @Column(name = "pain_level")
    private Integer painLevel; // 0-10 scale
    @CreationTimestamp
    @Column(name = "recorded_at")
    private LocalDate timestamp;
    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;
}
