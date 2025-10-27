package pain_helper_back.anesthesiologist.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;
import pain_helper_back.common.patients.dto.DrugRecommendationDTO;

import java.util.List;

@Data
public class AnesthesiologistRecommendationUpdateDTO {
    private List<DrugRecommendationDTO> drugs;
    private List<String> contraindications;
    @Size(max = 1000)
    private String comment; // обязательный пояснительный комментарий
}