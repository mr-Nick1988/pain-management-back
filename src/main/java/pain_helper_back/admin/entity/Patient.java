package pain_helper_back.admin.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity

@Data
public class Patient {
    @Id

    private Long id;
    private String name;
    private LocalDate dateOfBirth;
    private Double weight; // kg
    private Integer age;
    private String gfr; // Glomerular Filtration Rate
    private String childPughScore; // Liver function score
    private Double plt; // Platelet count
    private Double wbc; // White blood cell count
    private Double sat; // Oxygen saturation
    private Double sodium; // Sodium level

    // Add more EMR fields as needed
}
