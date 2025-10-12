package pain_helper_back.common.patients.dto;


import lombok.Data;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.enums.DrugRoute;
@Data
public class DrugRecommendationDTO {

    private String drugName;

    private String activeMoiety;            // Активное вещество в лекарстве

    private String dosing;

    private String interval;

    private DrugRoute route;                  // Путь введения, например "oral", "IV"
    private String ageAdjustment;           // Ограничения по возрасту
    private String weightAdjustment;
    private String childPugh;               // оценка влияния печёночной недостаточности
    private DrugRole role;                // основное лекарство или альтернативное

    // пока опциональное поле, не используется на фронте
    private String patientMrn; // пригодится для поисковых запросов без обёртки Patient,чтоб понять к кому относится
    // создать нужный метод в сервисе и не забыть присвоить это поле (поиск всех Paracetamol)
    // emrDto.setPatientMrn(emr.getPatient().getMrn());
}
