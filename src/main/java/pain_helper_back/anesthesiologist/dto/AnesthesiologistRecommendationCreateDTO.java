package pain_helper_back.anesthesiologist.dto;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pain_helper_back.common.patients.dto.DrugRecommendationDTO;
import java.util.List;

@Data
public class AnesthesiologistRecommendationCreateDTO {

    @NotNull
    private Long previousRecommendationId; // ID старой рекомендации (для связи)

    @NotNull
    private String patientMrn; // чтобы точно знать, кому создаём

    @NotNull
    private Integer regimenHierarchy; // линия терапии

    @NotEmpty
    private List<DrugRecommendationDTO> drugs; // препараты (main + alternative)

    private List<String> contraindications;
    private List<String> comments;
}