package pain_helper_back.pain_escalation_tracking.dto;

import lombok.Builder;
import lombok.Value;


import java.time.LocalDateTime;


/*
 * DTO-ответ после регистрации введённой дозы
 * Возвращается клиенту для отображения деталей сохранённой операции
 */
@Value
@Builder
public class DoseAdministrationResponseDTO {
    Long id;
    String patientMrn;
    Long recommendationId;
    String drugName;
    String dosage;
    String route;
    String administeredBy;
    Integer vasBefore;
    Integer vasAfter;
    String notes;
    LocalDateTime administeredAt;
}
