package pain_helper_back.common.patients.dto;


import lombok.Getter;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.enums.DrugRoute;
@Getter
public class DrugRecommendationDTO {

    private String drugName;

    private String activeMoiety;            // Активное вещество в лекарстве

    private String dosing;

    private String interval;

    private DrugRoute route;                  // Путь введения, наExample "oral", "IV"
    private String ageAdjustment;           // Ограничения по возрасту
    private String weightAdjustment;
    private String childPugh;               // оценка влияния печёночной недостаточности
    private DrugRole role;                // основное medicine or альтернативное

    // пока опциональное field, не используется на фронте
    private String patientMrn; // пригодится for поисковых requestов without обёртки Patient,чтоб понять к кому относится
    // создать нужный Method в Serviceе и не забыть присвоить это field (поиск allх Paracetamol)
    // emrDto.setPatientMrn(emr.getPatient().getMrn());
}
