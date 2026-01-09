package pain_helper_back.anesthesiologist.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import pain_helper_back.common.patients.dto.DrugRecommendationDTO;
import java.util.List;

@Getter
@Setter
public class AnesthesiologistRecommendationCreateDTO {

    @NotNull
    private Long previousRecommendationId; // ID of previous recommendation (for linking)

    @NotNull
    private String patientMrn; // To precisely identify patient

    @NotNull
    private Integer regimenHierarchy; // Treatment line

    @NotEmpty
    private List<DrugRecommendationDTO> drugs; // Drugs (main + alternative)

    private List<String> contraindications;
    private List<String> comments;
}
