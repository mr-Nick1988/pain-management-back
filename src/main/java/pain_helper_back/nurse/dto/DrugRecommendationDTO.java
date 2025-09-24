package pain_helper_back.nurse.dto;


public class DrugRecommendationDTO {

    private String DrugName;

    private String firstActiveMoiety;            // Активное вещество в лекарстве

    private String dosing;

    private String interval;

    private String route;                   // Путь введения, например "oral", "IV"
    private String ageAdjustment;           // Ограничения по возрасту
    private String weightAdjustment;
    private String childPugh;               // оценка влияния печёночной недостаточности
}
