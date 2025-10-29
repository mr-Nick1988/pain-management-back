package pain_helper_back.anesthesiologist.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.anesthesiologist.dto.AnesthesiologistRecommendationCreateDTO;
import pain_helper_back.anesthesiologist.dto.AnesthesiologistRecommendationUpdateDTO;
import pain_helper_back.anesthesiologist.service.AnesthesiologistServiceInterface;
import pain_helper_back.common.patients.dto.*;

import java.util.List;

@RestController
@RequestMapping("/api/anesthesiologist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnesthesiologistController {

    private final AnesthesiologistServiceInterface anesthesiologistService;

    // Escalation endpoints

    @GetMapping("/escalations")
    public List<RecommendationWithVasDTO> getAllEscalations() {
        return anesthesiologistService.getAllEscalations();
    }

    // Rejected endpoints
    @GetMapping("/recommendations/rejected")
    public List<RecommendationWithVasDTO> getRejectedRecommendations() {
        return anesthesiologistService.getRejectedRecommendations();
    }


    //  APPROVE/REJECT ЭСКАЛАЦИИ ========== //

    @PostMapping("/recommendations/{recommendationId}/approve")
    public RecommendationDTO approveRecommendation(
            @PathVariable Long recommendationId,
            @Valid @RequestBody RecommendationApprovalRejectionDTO dto) {
        return anesthesiologistService.approveEscalation(recommendationId, dto);
    }

    @PostMapping("/recommendations/{recommendationId}/reject")
    public RecommendationDTO rejectRecommendation(
            @PathVariable Long recommendationId,
            @Valid @RequestBody RecommendationApprovalRejectionDTO dto) {
        return anesthesiologistService.rejectEscalation(recommendationId, dto);
    }



    // ================= PROTOCOL ENDPOINTS ================= //
    @PostMapping("/recommendations")
    public RecommendationDTO createRecommendationAfterRejection(
            @Valid @RequestBody AnesthesiologistRecommendationCreateDTO dto) {
        return anesthesiologistService.createRecommendationAfterRejection(dto);
    }

    //// Позволяет анестезиологу откорректировать существующую рекомендацию (например, дозу или интервал)
    //// без полного её отклонения. После апдейта рекомендация снова становится APPROVED.
    @PutMapping("/recommendations/{id}/update")
    public RecommendationDTO updateRecommendation(
            @PathVariable Long id,
            @Valid @RequestBody AnesthesiologistRecommendationUpdateDTO dto) {
        return anesthesiologistService.updateRecommendation(id, dto);
    }

    // ================= PATIENTS ================= //

    @GetMapping("/patients/mrn/{mrn}")
    public PatientDTO getPatientByMrn(@PathVariable String mrn) {
        return anesthesiologistService.getPatientByMrn(mrn);
    }

// ================= EMR ================= //

    @GetMapping("/patients/{mrn}/emr/last")
    public EmrDTO getLastEmrByPatientMrn(@PathVariable String mrn) {
        return anesthesiologistService.getLastEmrByPatientMrn(mrn);
    }


    @GetMapping("/patients/{mrn}/history")
    public List<RecommendationWithVasDTO> getPatientHistory(@PathVariable String mrn) {
        return anesthesiologistService.getRecommendationsWithVasByPatientMrn(mrn);
    }

    //TODO (A) Fallback #1 — восстановление контекста рекомендации


}
