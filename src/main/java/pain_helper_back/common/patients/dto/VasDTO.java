package pain_helper_back.common.patients.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VasDTO {
    @Size(max = 25, message = "Pain place must be at most 25 characters long")
    private String painPlace;
    @NotNull(message = "Pain level is required")
    @Min(value = 0, message = "Pain level must be at least 0")
    @Max(value = 10, message = "Pain level must be at most 10")
    private Integer painLevel;
    private boolean resolved;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String updatedBy;
    private String createdBy;

    // пока опциональное поле, не используется на фронте
    private String patientMrn; // пригодится для поисковых запросов без обёртки Patient,чтоб понять к кому относится
    // создать нужный метод в сервисе и не забыть присвоить это поле (поиск всех EMR с gfr<60)
    // emrDto.setPatientMrn(emr.getPatient().getMrn());
}
