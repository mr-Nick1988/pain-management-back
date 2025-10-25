package pain_helper_back.common.patients.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class RecommendationDTO {

    private Long id;

    @NotNull(message = "Regimen hierarchy is required")
    private int regimenHierarchy; // первая линия лекарства, вторая, если не помогло и т.д.
    private RecommendationStatus status;     // Статус рекомендации: pending, approved, rejected
    private String rejectedReason;

    private List<DrugRecommendationDTO> drugs; // Здесь будут 1-е, 2-е и т.д. препараты
    private List<String> contraindications;    // противопоказания


    private List<String> comments;      // свободные комментарии

    private Boolean generationFailed;   // если ни одна рекомендация не подошла
    List<String> rejectionReasonsSummary;     // список всех причин отказа (system)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String createdBy;

    // пока опциональное поле, не используется на фронте
    private String patientMrn; // пригодится для поисковых запросов без обёртки Patient,чтоб понять к кому относится


    // создать нужный метод в сервисе и не забыть присвоить это поле (поиск всех активных рекомендаций)
    // emrDto.setPatientMrn(emr.getPatient().getMrn());

}
