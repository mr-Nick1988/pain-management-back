package pain_helper_back.emr_integration.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

/*
 * DTO для лабораторного показателя из FHIR Observation ресурса.
 *
 * FHIR Observation используется для хранения:
 * - Лабораторных анализов (кровь, моча и т.д.)
 * - Витальных показателей (давление, пульс, сатурация)
 * - Измерений (вес, рост, температура)
 *
 * Для PMA критичны:
 * - GFR (функция почек) - LOINC 2160-0
 * - PLT (тромбоциты) - LOINC 777-3
 * - WBC (лейкоциты) - LOINC 6690-2
 * - Sodium (натрий) - LOINC 2951-2
 * - SpO2 (сатурация) - LOINC 59408-5
 * - Creatinine (креатинин) - LOINC 2160-0
 * - Bilirubin (билирубин) - LOINC 1975-2
 *
 * FHIR Observation: http://hl7.org/fhir/observation.html
 * LOINC codes: https://loinc.org/
 */
@Data
public class FhirObservationDTO {
    //ID наблюдения в FHIR системе.
    @NotBlank(message = "FHIR resource ID is required")
    @Size(max = 100,message = "FHIR resource ID must not exceed 100 characters")
    private String fhirObservationInResourceId;
    //LOINC код показателя (стандартный код для лабораторных анализов).
    //Например: "2160-0" для креатинина, "777-3" для тромбоцитов.
    @Size(max = 50,message = "LOINC code must not exceed 50 characters")
    private String loincCode;
    //Название показателя (человекочитаемое).Например: "Creatinine", "Platelets", "White Blood Cells".
    @Size(max = 200,message = "Display name must not exceed 200 characters")
    private String displayName;
    //Значение показателя (числовое).
    private Double value;
    //Единица измерения (mg/dL, mmol/L, 10*3/uL и т.д.).
    @Size(max = 50,message = "Unit must not exceed 50 characters")
    private String unit;
    //Дата и время измерения.
    private LocalDateTime effectiveDateTime;
    //Статус наблюдения (final, preliminary, amended и т.д.).
    @Size(max = 20, message = "Status must not exceed 20 characters")
    private String status;
    //Референсный диапазон (нормальные значения) - нижняя граница.
    private Double referenceRangeLow;
    // Референсный диапазон (нормальные значения) - верхняя граница.
    private Double referenceRangeHigh;
    //Интерпретация результата (normal, high, low, critical и т.д.).
    @Size(max = 50, message = "Interpretation must not exceed 50 characters")
    private String interpretationOfResult;
    //ID пациента, к которому относится наблюдение.
    @NotBlank(message = "Patient reference is required")
    @Size(max = 100, message = "Patient reference must not exceed 100 characters")
    private String patientReference;

}
