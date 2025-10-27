package pain_helper_back.anesthesiologist.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.anesthesiologist.dto.*;
import pain_helper_back.anesthesiologist.service.AnesthesiologistServiceInterface;
import pain_helper_back.common.patients.dto.RecommendationDTO;
import pain_helper_back.common.patients.dto.RecommendationWithVasDTO;
import pain_helper_back.common.patients.dto.RecommendationApprovalRejectionDTO;

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


    //TODO (A) Fallback #1 — восстановление контекста рекомендации
    //TODO Fallback #2 — проверка и напоминание о невыполненном Reject







}
