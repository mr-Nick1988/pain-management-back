package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pain_helper_back.nurse.PatientsGenders;

import java.time.LocalDate;
import java.util.List;

@Data
public class PatientUpdateDto {

    private String firstName;
    private String lastName;
    private PatientsGenders gender;
    private Double height;
    private Double weight;


    private List<EmrDto> emr;

    private List<VasDto> vas;

    private List<RecommendationDto> recommendations;


}
