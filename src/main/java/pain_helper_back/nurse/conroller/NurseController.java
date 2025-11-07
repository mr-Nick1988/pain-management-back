package pain_helper_back.nurse.conroller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.nurse.service.NurseService;
import pain_helper_back.security.JwtAuthenticationFilter;
import pain_helper_back.security.RequireRole;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/nurse")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
@Slf4j
public class NurseController {
    private final NurseService nurseService;

    @PostMapping("/patients")
    @RequireRole({"NURSE", "ADMIN"})
    public PatientDTO createPatient(
            @Valid @RequestBody PatientDTO patientDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/nurse/patients - createdBy={}", userDetails.getPersonId());
        return nurseService.createPatient(patientDto);
    }

    @GetMapping("/patients/mrn/{mrn}")
    @RequireRole({"NURSE", "ADMIN"})
    public PatientDTO getPatientByMrn(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/nurse/patients/mrn/{} - requestedBy={}", mrn, userDetails.getPersonId());
        return nurseService.getPatientByMrn(mrn);
    }

    @GetMapping("/patients/email/{email}")
    @RequireRole({"NURSE", "ADMIN"})
    public PatientDTO getPatientByEmail(
            @PathVariable String email,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/nurse/patients/email/{} - requestedBy={}", email, userDetails.getPersonId());
        return nurseService.getPatientByEmail(email);
    }


    @GetMapping("/patients/phoneNumber/{phoneNumber}")
    @RequireRole({"NURSE", "ADMIN"})
    public PatientDTO getPatientByPhoneNumber(
            @PathVariable String phoneNumber,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/nurse/patients/phoneNumber/{} - requestedBy={}",
                phoneNumber, userDetails.getPersonId());
        return nurseService.getPatientByPhoneNumber(phoneNumber);
    }

    @GetMapping("/patients")
    @RequireRole({"NURSE", "ADMIN"})
    public List<PatientDTO> getPatients(
            @RequestParam(required = false) String firstName,
            @RequestParam(required = false) String lastName,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) @DateTimeFormat(pattern = "dd-MM-yyyy") LocalDate birthDate,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/nurse/patients - requestedBy={}", userDetails.getPersonId());
        return nurseService.searchPatients(firstName, lastName, isActive, birthDate);
    }

    @DeleteMapping("/patients/{mrn}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireRole({"NURSE", "ADMIN"})
    public void deletePatient(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("DELETE /api/nurse/patients/{} - deletedBy={}", mrn, userDetails.getPersonId());
        nurseService.deletePatient(mrn);
    }

    @PatchMapping("/patients/{mrn}")
    @RequireRole({"NURSE", "ADMIN"})
    public PatientDTO updatePatient(
            @PathVariable String mrn,
            @Valid @RequestBody PatientUpdateDTO patientUpdateDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("PATCH /api/nurse/patients/{} - updatedBy={}", mrn, userDetails.getPersonId());
        return nurseService.updatePatient(mrn, patientUpdateDto);
    }

    @PostMapping("/patients/{mrn}/emr")
    @RequireRole({"NURSE", "ADMIN"})
    public EmrDTO createEmr(
            @PathVariable String mrn,
            @Valid @RequestBody EmrDTO emrDTO,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/nurse/patients/{}/emr - createdBy={}", mrn, userDetails.getPersonId());
        return nurseService.createEmr(mrn, emrDTO);
    }

    @GetMapping("/patients/{mrn}/emr")
    @RequireRole({"NURSE", "ADMIN"})
    public EmrDTO getEmrByPatientMrn(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/nurse/patients/{}/emr - requestedBy={}", mrn, userDetails.getPersonId());
        return nurseService.getLastEmrByPatientMrn(mrn);
    }

    @PatchMapping("/patients/{mrn}/emr")
    @RequireRole({"NURSE", "ADMIN"})
    public EmrDTO updateEmr(
            @PathVariable String mrn,
            @Valid @RequestBody EmrUpdateDTO emrUpdateDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("PATCH /api/nurse/patients/{}/emr - updatedBy={}", mrn, userDetails.getPersonId());
        return nurseService.updateEmr(mrn, emrUpdateDto);
    }

    @PostMapping("/patients/{mrn}/vas")
    @RequireRole("NURSE")
    public VasDTO createVAS(
            @PathVariable String mrn,
            @Valid @RequestBody VasDTO vasDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/nurse/patients/{}/vas - createdBy={}", mrn, userDetails.getPersonId());
        return nurseService.createVAS(mrn, vasDto);
    }

    @PatchMapping("/patients/{mrn}/vas")
    @RequireRole("NURSE")
    public VasDTO updateVAS(
            @PathVariable String mrn,
            @Valid @RequestBody VasDTO vasDto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("PATCH /api/nurse/patients/{}/vas - updatedBy={}", mrn, userDetails.getPersonId());
        return nurseService.updateVAS(mrn, vasDto);
    }

    @DeleteMapping("/patients/{mrn}/vas")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @RequireRole("NURSE")
    public void deleteVAS(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("DELETE /api/nurse/patients/{}/vas - deletedBy={}", mrn, userDetails.getPersonId());
        nurseService.deleteVAS(mrn);
    }

    @GetMapping("patients/{mrn}/vas")
    @RequireRole({"NURSE", "ADMIN"})
    public Optional<VasDTO> getLastVAS(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/nurse/patients/{}/vas - requestedBy={}", mrn, userDetails.getPersonId());
        return nurseService.getLastVAS(mrn);
    }


    @GetMapping("/recommendations/approved")
    @RequireRole({"NURSE", "ADMIN"})
    public List<RecommendationDTO> getAllPendingRecommendations(Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/nurse/recommendations/approved - requestedBy={}", userDetails.getPersonId());
        return nurseService.getAllApprovedRecommendations();
    }


    @PostMapping("/patients/{mrn}/recommendation")
    @RequireRole("NURSE")
    public RecommendationDTO createRecommendation(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/nurse/patients/{}/recommendation - createdBy={}",
                mrn, userDetails.getPersonId());
        return nurseService.createRecommendation(mrn);
    }

    @PostMapping("/patients/{mrn}/recommendation/execute")
    @RequireRole("NURSE")
    public RecommendationDTO executeRecommendation(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/nurse/patients/{}/recommendation/execute - executedBy={}",
                mrn, userDetails.getPersonId());
        return nurseService.executeRecommendation(mrn);
    }

    @GetMapping("/patients/{mrn}/recommendation")
    @RequireRole({"NURSE", "ADMIN"})
    public Optional<RecommendationDTO> getLastRecommendation(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/nurse/patients/{}/recommendation - requestedBy={}",
                mrn, userDetails.getPersonId());
        return nurseService.getLastRecommendation(mrn);
    }
}