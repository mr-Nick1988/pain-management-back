package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class RecommendationDTO {

    @NotNull(message = "Regimen hierarchy is required")
    private int regimenHierarchy; // первая линия лекарства, вторая ,если не помогло и т.д.

    private List<DrugRecommendationDTO> drugs; // здесь будут 1-е, 2-е и т.д. препараты
    private List<DrugRecommendationDTO> alternativeDrugs; // здесь будут альтернативные препараты

    private List<String> avoidIfSensitivity;   // избегать если аллергия
    private List<String> contraindications;    // противопоказания

    private RecommendationStatus status;     // Статус рекомендации: pending, approved, rejected
    private List<String> notes;      // свободные комментарии
    private LocalDateTime createdAt = LocalDateTime.now();



}
