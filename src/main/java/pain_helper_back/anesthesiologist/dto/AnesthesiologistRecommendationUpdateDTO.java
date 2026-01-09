package pain_helper_back.anesthesiologist.dto;

import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import pain_helper_back.common.patients.dto.DrugRecommendationDTO;

import java.util.List;

@Getter
@Setter
public class AnesthesiologistRecommendationUpdateDTO {
    private List<DrugRecommendationDTO> drugs;
    private List<String> contraindications;
    @Size(max = 1000)
    private String comment; // Required explanatory comment
}
