package pain_helper_back.emr_integration.dto;

/*
 * DTO для пациента из FHIR системы.
 * Упрощенное представление FHIR Patient ресурса с полями, необходимыми для PMA.
 *
 * FHIR Patient resource: http://hl7.org/fhir/patient.html
 */

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import lombok.Data;
import pain_helper_back.enums.EmrSourceType;

import java.time.LocalDate;
import java.util.List;

@Data
public class FhirPatientDTO {

    //ID for patient in FHIR system
    @NotBlank(message = "FHIR Patient ID is required")
    @Size(max = 100, message = "FHIR Patient ID must be less than 100 characters")
    private String patientIdInFhirResource;
    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;
    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;
    @Past(message = "Date of birth must be in the past")
    private LocalDate dateOfBirth;
    @Size(max = 20, message = "Gender must not exceed 20 characters")
    private String gender;
    /**
     * Идентификаторы пациента из FHIR системы (MRN, SSN, страховой полис и т.д.).
     * FHIR Patient.identifier - может быть несколько идентификаторов разных типов.
     */
    private List<FhirIdentifierDTO> identifiers;
    @Size(max = 20, message = "Phone number must not exceed 20 characters")
    private String phoneNumber;
    @Size(max = 100, message = "Email must not exceed 100 characters")
    private String email;
    @Size(max = 500, message = "Address must not exceed 500 characters")
    private String address;

    //URL of the source FHIR system
    @Size(max = 200, message = "Source system URL must not exceed 200 characters")
    private String sourceSystemUrl;
    
    // Источник данных (FHIR_SERVER, MOCK_GENERATOR, EXTERNAL_HOSPITAL, MANUAL_ENTRY)
    private EmrSourceType sourceType;
}
