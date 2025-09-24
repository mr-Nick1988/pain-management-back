package pain_helper_back.nurse.dto;

import lombok.Data;
import pain_helper_back.nurse.PatientsGenders;

import java.util.List;

@Data
public class PatientUpdateDTO {

    private String firstName;
    private String lastName;
    private PatientsGenders gender;
    private Double height;
    private Double weight;


    private List<EmrDTO> emr;

    private List<VasDTO> vas;

    private List<RecommendationDTO> recommendations;


}
