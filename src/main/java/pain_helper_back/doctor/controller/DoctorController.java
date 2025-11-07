package pain_helper_back.doctor.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.doctor.service.DoctorService;
import pain_helper_back.security.JwtAuthenticationFilter;
import pain_helper_back.security.RequireRole;

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
    @RequireRole({"DOCTOR", "ADMIN"})
    public PatientDTO createPatient(
            @RequestBody @Valid PatientDTO patientDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/doctor/patients - createdBy={}", userDetails.getPersonId());
        return doctorService.createPatient(patientDto);
    }

    @GetMapping("/patients/mrn/{mrn}")
    @RequireRole({"DOCTOR", "ADMIN"})
    public PatientDTO getPatientByMrn(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/patients/mrn/{} - requestedBy={}", mrn, userDetails.getPersonId());
        return doctorService.getPatientByMrn(mrn);
    }

    @GetMapping("/patients/email/{email}")
    @RequireRole({"DOCTOR", "ADMIN"})
    public PatientDTO getPatientByEmail(
            @PathVariable String email,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/patients/email/{} - requestedBy={}", email, userDetails.getPersonId());
        return doctorService.getPatientByEmail(email);
    }

    @GetMapping("/patients/phone/{phoneNumber}")
    @RequireRole({"DOCTOR", "ADMIN"})
    public PatientDTO getPatientByPhoneNumber(
            @PathVariable String phoneNumber,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/patients/phone/{} - requestedBy={}", phoneNumber, userDetails.getPersonId());
        return doctorService.getPatientByPhoneNumber(phoneNumber);
    }

    @GetMapping("/patients")
    @RequireRole({"DOCTOR", "ADMIN"})
    public List<PatientDTO> searchPatients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate birthDate,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) String insurancePolicyNumber,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String phoneNumber,
            @RequestParam(required = false) String email,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/patients - requestedBy={}", userDetails.getPersonId());
        return doctorService.searchPatients(firstName, lastName, isActive, birthDate, gender, insurancePolicyNumber, address, phoneNumber, email);
    }

    @DeleteMapping("/patients/{mrn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireRole({"DOCTOR", "ADMIN"})
    public void deletePatient(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("DELETE /api/doctor/patients/{} - deletedBy={}", mrn, userDetails.getPersonId());
        doctorService.deletePatient(mrn);
    }

    @PatchMapping("/patients/{mrn}")
    @RequireRole({"DOCTOR", "ADMIN"})
    public PatientDTO updatePatient(
            @PathVariable String mrn,
            @RequestBody @Valid PatientUpdateDTO patientUpdateDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("PATCH /api/doctor/patients/{} - updatedBy={}", mrn, userDetails.getPersonId());
        return doctorService.updatePatient(mrn, patientUpdateDto);
    }

    // ================= EMR ================= //

    @PostMapping("/patients/{mrn}/emr")
    @RequireRole({"DOCTOR", "ADMIN"})
    public EmrDTO createEmr(
            @PathVariable String mrn,
            @RequestBody @Valid EmrDTO emrDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/doctor/patients/{}/emr - createdBy={}", mrn, userDetails.getPersonId());
        return doctorService.createEmr(mrn, emrDto);
    }

    @GetMapping("/patients/{mrn}/emr/last")
    @RequireRole({"DOCTOR", "ADMIN"})
    public EmrDTO getLastEmrByPatientMrn(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/patients/{}/emr/last - requestedBy={}", mrn, userDetails.getPersonId());
        return doctorService.getLastEmrByPatientMrn(mrn);
    }

    @PatchMapping("/patients/{mrn}/emr")
    @RequireRole({"DOCTOR", "ADMIN"})
    public EmrDTO updateEmr(
            @PathVariable String mrn,
            @RequestBody @Valid EmrUpdateDTO emrUpdateDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("PATCH /api/doctor/patients/{}/emr - updatedBy={}", mrn, userDetails.getPersonId());
        return doctorService.updateEmr(mrn, emrUpdateDto);
    }

    @GetMapping("/patients/{mrn}/emr")
    @RequireRole({"DOCTOR", "ADMIN"})
    public List<EmrDTO> getAllEmrByPatientMrn(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/patients/{}/emr - requestedBy={}", mrn, userDetails.getPersonId());
        return doctorService.getAllEmrByPatientMrn(mrn);
    }

    // ================= RECOMMENDATIONS ================= //

    @GetMapping("/recommendations/pending")
    @RequireRole({"DOCTOR", "ADMIN"})
    public List<RecommendationWithVasDTO> getAllPendingRecommendations(Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/recommendations/pending - requestedBy={}", userDetails.getPersonId());
        return doctorService.getAllPendingRecommendations();
    }

    @GetMapping("/patients/{mrn}/recommendations/last")
    @RequireRole({"DOCTOR", "ADMIN"})
    public RecommendationWithVasDTO getLastRecommendationByMrn(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/patients/{}/recommendations/last - requestedBy={}", mrn, userDetails.getPersonId());
        return doctorService.getLastRecommendationByMrn(mrn);
    }
    @PostMapping("/recommendations/{recommendationId}/approve")
    @RequireRole("DOCTOR")
    public RecommendationDTO approveRecommendation(
            @PathVariable Long recommendationId,
            @RequestBody @Valid RecommendationApprovalRejectionDTO dto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/doctor/recommendations/{}/approve - approvedBy={}", recommendationId, userDetails.getPersonId());
        return doctorService.approveRecommendation(recommendationId, dto);
    }

    @PostMapping("/recommendations/{recommendationId}/reject")
    @RequireRole("DOCTOR")
    public RecommendationDTO rejectRecommendation(
            @PathVariable Long recommendationId,
            @RequestBody @Valid RecommendationApprovalRejectionDTO dto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/doctor/recommendations/{}/reject - rejectedBy={}", recommendationId, userDetails.getPersonId());
        return doctorService.rejectRecommendation(recommendationId, dto);
    }

    // ===  get all recommendations with VAS history ===
    @GetMapping("/patients/{mrn}/history")
    @RequireRole({"DOCTOR", "ADMIN"})
    public List<RecommendationWithVasDTO> getPatientHistory(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/doctor/patients/{}/history - requestedBy={}", mrn, userDetails.getPersonId());
        return doctorService.getRecommendationsWithVasByPatientMrn(mrn);
    }

}