package pain_helper_back.external_emr_integration_service.dto;




/*
 * DTO для идентификатора пациента из FHIR системы.
 *
 * В FHIR у пациента может быть несколько идентификаторов:
 * - MRN (Medical Record Number) - номер медицинской карты
 * - Insurance Policy Number - номер страхового полиса
 * и т.д.
 *
 * FHIR Identifier: http://hl7.org/fhir/datatypes.html#Identifier
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class FhirIdentifierDTO {

    /**
     * Тип идентификатора (MRN, SSN, insurance и т.д.).
     * Обычно это код из FHIR CodeSystem.
     */
    @Size(max =50,message = "Identifier type must not exceed 50 characters")
    private String type;

    //Система которая выдала идентификатор(например , URL ,больницы)
    @Size(max = 200, message = "Identifier value must not exceed 200 characters")
    private String system;
    //Значение идентификатора.
    @NotBlank(message = "Identifier value must not be blank")
    @Size(max = 200, message = "Identifier value must not exceed 200 characters")
    private String value;
    //Использование идентификатора (official, temp, old и т.д.).
    @Size(max = 50, message = "Identifier use must not exceed 50 characters")
    private String use;


}


