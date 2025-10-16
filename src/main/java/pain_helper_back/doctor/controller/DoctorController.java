package pain_helper_back.doctor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.doctor.dto.*;
import pain_helper_back.doctor.service.DoctorService;

import java.time.LocalDate;
import java.util.List;

/**
 * Controller for handling doctor-related operations.
 */
@RestController
@RequestMapping("api/doctor")
@CrossOrigin(origins = "http://localhost:5173")
@RequiredArgsConstructor
@Slf4j
public class DoctorController {

    private final DoctorService doctorService;

    // ================= PATIENTS ================= //

    @PostMapping("/patients")
    public PatientDTO createPatient(@RequestBody @Valid PatientDTO patientDto) {
        return doctorService.createPatient(patientDto);
    }

    @GetMapping("/patients/mrn/{mrn}")
    public PatientDTO getPatientByMrn(@PathVariable String mrn) {
        return doctorService.getPatientByMrn(mrn);
    }

    @GetMapping("/patients/email/{email}")
    public PatientDTO getPatientByEmail(@PathVariable String email) {
        return doctorService.getPatientByEmail(email);
    }

    @GetMapping("/patients/phone/{phoneNumber}")
    public PatientDTO getPatientByPhoneNumber(@PathVariable String phoneNumber) {
        return doctorService.getPatientByPhoneNumber(phoneNumber);
    }

    @GetMapping("/patients")
    public List<PatientDTO> searchPatients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate birthDate,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String insurancePolicyNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String email
    ) {

        return doctorService.searchPatients(firstName, lastName, isActive, birthDate, gender, insurancePolicyNumber, address, phoneNumber, email);
    }

    @DeleteMapping("/patients/{mrn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePatient(@PathVariable String mrn) {
        doctorService.deletePatient(mrn);
    }

    @PatchMapping("/patients/{mrn}")
    public PatientDTO updatePatient(@PathVariable String mrn, @RequestBody @Valid PatientUpdateDTO patientUpdateDto) {
        return doctorService.updatePatient(mrn, patientUpdateDto);
    }

    // ================= EMR ================= //

    @PostMapping("/patients/{mrn}/emr")
    public EmrDTO createEmr(@PathVariable String mrn, @RequestBody @Valid EmrDTO emrDto) {
        return doctorService.createEmr(mrn, emrDto);
    }

    @GetMapping("/patients/{mrn}/emr/last")
    public EmrDTO getLastEmrByPatientMrn(@PathVariable String mrn) {
        return doctorService.getLastEmrByPatientMrn(mrn);
    }

    @PatchMapping("/patients/{mrn}/emr")
    public EmrDTO updateEmr(@PathVariable String mrn, @RequestBody @Valid EmrUpdateDTO emrUpdateDto) {
        return doctorService.updateEmr(mrn, emrUpdateDto);
    }

    @GetMapping("/patients/{mrn}/emr")
    public List<EmrDTO> getAllEmrByPatientMrn(@PathVariable String mrn) {
        return doctorService.getAllEmrByPatientMrn(mrn);
    }

    // ================= RECOMMENDATIONS ================= //

    @GetMapping("/recommendations/pending")
    public List<RecommendationWithVasDTO> getAllPendingRecommendations() {
        return doctorService.getAllPendingRecommendations();
    }

    @GetMapping("/patients/{mrn}/recommendations/last")
    public RecommendationWithVasDTO getLastRecommendationByMrn(@PathVariable String mrn) {
        return doctorService.getLastRecommendationByMrn(mrn);
    }
    @PostMapping("/recommendations/{recommendationId}/approve")
    public RecommendationDTO approveRecommendation(
            @PathVariable Long recommendationId,
            @RequestBody @Valid RecommendationApprovalRejectionDTO dto
    ) {
        return doctorService.approveRecommendation(recommendationId, dto);
    }

    @PostMapping("/recommendations/{recommendationId}/reject")
    public RecommendationDTO rejectRecommendation(
            @PathVariable Long recommendationId,
            @RequestBody @Valid RecommendationApprovalRejectionDTO dto
    ) {
        return doctorService.rejectRecommendation(recommendationId, dto);
    }

}