package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.NotBlank;

public class DrugRecommendationDto {

    @NotBlank(message = "Drug name is required")
    private String drugName;
    @NotBlank(message = "Drug active moiety is required")
    private String activeMoiety;            // Активное вещество в лекарстве
    @NotBlank(message = "Dosage is required")
    private String dosage;
    @NotBlank(message = "Interval is required")
    private String interval;
    @NotBlank(message = "Route is required")
    private String route;                   // Путь введения, например "oral", "IV"
    private String ageAdjustment;           // Ограничения по возрасту
    private String weightAdjustment;
    private String childPugh;               // оценка влияния печёночной недостаточности
}
