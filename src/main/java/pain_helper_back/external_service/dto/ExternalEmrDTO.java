package pain_helper_back.external_service.dto;
import lombok.Data;
import java.time.LocalDate;

// --------> Примерный вид медицинских данных из HIPI KFIR Patient API
@Data
public class ExternalEmrDTO {
    private String externalId;       // ID пациента во внешней системе
    private String fullName;         // ФИО (там часто одно поле)
    private LocalDate dateOfBirth;
    private String gender;
    private String diagnosis;        // основной диагноз
    private Double hemoglobin;       // гемоглобин (пример лабораторного показателя)
    private Double creatinine;       // креатинин (для оценки почек)
    private Double bilirubin;        // билирубин (для печени)
    private LocalDate recordDate;
}
