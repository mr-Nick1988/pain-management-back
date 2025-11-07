package pain_helper_back.anesthesiologist.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.anesthesiologist.dto.AnesthesiologistRecommendationCreateDTO;
import pain_helper_back.anesthesiologist.dto.AnesthesiologistRecommendationUpdateDTO;
import pain_helper_back.anesthesiologist.service.AnesthesiologistServiceInterface;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.security.JwtAuthenticationFilter;
import pain_helper_back.security.RequireRole;

import java.util.List;

@RestController
@RequestMapping("/api/anesthesiologist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
@Slf4j
public class AnesthesiologistController {

    private final AnesthesiologistServiceInterface anesthesiologistService;

    @GetMapping("/escalations")
    @RequireRole("ANESTHESIOLOGIST")
    public List<RecommendationWithVasDTO> getAllEscalations(Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/anesthesiologist/escalations - requestedBy={}", userDetails.getPersonId());
        return anesthesiologistService.getAllEscalations();
    }

    @GetMapping("/recommendations/rejected")
    @RequireRole("ANESTHESIOLOGIST")
    public List<RecommendationWithVasDTO> getRejectedRecommendations(Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/anesthesiologist/recommendations/rejected - requestedBy={}", userDetails.getPersonId());
        return anesthesiologistService.getRejectedRecommendations();
    }

    @PostMapping("/recommendations/{recommendationId}/approve")
    @RequireRole("ANESTHESIOLOGIST")
    public RecommendationDTO approveRecommendation(
            @PathVariable Long recommendationId,
            @Valid @RequestBody RecommendationApprovalRejectionDTO dto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/anesthesiologist/recommendations/{}/approve - approvedBy={}",
                recommendationId, userDetails.getPersonId());
        return anesthesiologistService.approveEscalation(recommendationId, dto);
    }

    @PostMapping("/recommendations/{recommendationId}/reject")
    @RequireRole("ANESTHESIOLOGIST")
    public RecommendationDTO rejectRecommendation(
            @PathVariable Long recommendationId,
            @Valid @RequestBody RecommendationApprovalRejectionDTO dto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/anesthesiologist/recommendations/{}/reject - rejectedBy={}",
                recommendationId, userDetails.getPersonId());
        return anesthesiologistService.rejectEscalation(recommendationId, dto);
    }

    @PostMapping("/recommendations")
    @RequireRole("ANESTHESIOLOGIST")
    public RecommendationDTO createRecommendationAfterRejection(
            @Valid @RequestBody AnesthesiologistRecommendationCreateDTO dto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("POST /api/anesthesiologist/recommendations - createdBy={}", userDetails.getPersonId());
        return anesthesiologistService.createRecommendationAfterRejection(dto);
    }

    @PutMapping("/recommendations/{id}/update")
    @RequireRole("ANESTHESIOLOGIST")
    public RecommendationDTO updateRecommendation(
            @PathVariable Long id,
            @Valid @RequestBody AnesthesiologistRecommendationUpdateDTO dto,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("PUT /api/anesthesiologist/recommendations/{}/update - updatedBy={}", id, userDetails.getPersonId());
        return anesthesiologistService.updateRecommendation(id, dto);
    }

    @GetMapping("/patients/mrn/{mrn}")
    @RequireRole({"ANESTHESIOLOGIST", "ADMIN"})
    public PatientDTO getPatientByMrn(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/anesthesiologist/patients/mrn/{} - requestedBy={}", mrn, userDetails.getPersonId());
        return anesthesiologistService.getPatientByMrn(mrn);
    }

    @GetMapping("/patients/{mrn}/emr/last")
    @RequireRole({"ANESTHESIOLOGIST", "ADMIN"})
    public EmrDTO getLastEmrByPatientMrn(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/anesthesiologist/patients/{}/emr/last - requestedBy={}", mrn, userDetails.getPersonId());
        return anesthesiologistService.getLastEmrByPatientMrn(mrn);
    }

    @GetMapping("/patients/{mrn}/history")
    @RequireRole({"ANESTHESIOLOGIST", "ADMIN"})
    public List<RecommendationWithVasDTO> getPatientHistory(
            @PathVariable String mrn,
            Authentication authentication) {
        JwtAuthenticationFilter.UserDetails userDetails =
                (JwtAuthenticationFilter.UserDetails) authentication.getDetails();
        log.info("GET /api/anesthesiologist/patients/{}/history - requestedBy={}", mrn, userDetails.getPersonId());
        return anesthesiologistService.getRecommendationsWithVasByPatientMrn(mrn);
    }
}