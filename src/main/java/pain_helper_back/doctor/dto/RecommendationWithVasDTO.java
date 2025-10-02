package pain_helper_back.doctor.dto;

import lombok.Data;
import pain_helper_back.common.patients.dto.RecommendationDTO;
import pain_helper_back.common.patients.dto.VasDTO;

@Data
public class RecommendationWithVasDTO {
    private RecommendationDTO recommendation;
    private VasDTO vas;
}
