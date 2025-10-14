package pain_helper_back.anesthesiologist.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.anesthesiologist.dto.*;
import pain_helper_back.anesthesiologist.service.AnesthesiologistServiceInterface;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.util.List;

@RestController
@RequestMapping("/api/anesthesiologist")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class AnesthesiologistController {

    private final AnesthesiologistServiceInterface anesthesiologistService;

    // Escalation endpoints
    @GetMapping("/escalations")
    public List<EscalationResponseDTO> getAllEscalations() {
        return anesthesiologistService.getAllEscalations();
    }

    @GetMapping("/escalations/status/{status}")
    public List<EscalationResponseDTO> getEscalationsByStatus(@PathVariable EscalationStatus status) {
        return anesthesiologistService.getEscalationsByStatus(status);
    }

    @GetMapping("/escalations/priority/{priority}")
    public List<EscalationResponseDTO> getEscalationsByPriority(@PathVariable EscalationPriority priority) {
        return anesthesiologistService.getEscalationsByPriority(priority);
    }

    @GetMapping("/escalations/active")
    public List<EscalationResponseDTO> getActiveEscalationsOrderedByPriority() {
        return anesthesiologistService.getActiveEscalationsOrderedByPriority();
    }

    @GetMapping("/escalations/stats")
    public EscalationStatsDTO getEscalationStats() {
        return anesthesiologistService.getEscalationStats();
    }


    // ========== НОВЫЕ ENDPOINTS: APPROVE/REJECT ЭСКАЛАЦИИ ========== //

    @PostMapping("/escalations/{id}/approve")
    public EscalationResponseDTO approveEscalation(
            @PathVariable Long id,
            @Valid @RequestBody EscalationResolutionDTO resolutionDTO) {
        return anesthesiologistService.approveEscalation(id, resolutionDTO);
    }
    @PostMapping("/escalations/{id}/reject")
    public EscalationResponseDTO rejectEscalation(
            @PathVariable Long id,
            @Valid @RequestBody EscalationResolutionDTO resolutionDTO) {
        return anesthesiologistService.rejectEscalation(id, resolutionDTO);
    }
    // ================= PROTOCOL ENDPOINTS ================= //
    @PostMapping("/protocols")
    public ProtocolResponseDTO createProtocol(@Valid @RequestBody ProtocolRequestDTO protocolRequest) {
        return anesthesiologistService.createProtocol(protocolRequest);
    }
    @PutMapping("/protocols/{id}/approve")
    public ProtocolResponseDTO approveProtocol(
            @PathVariable Long id,
            @RequestParam(defaultValue = "system") String approvedBy) {
        return anesthesiologistService.approveProtocol(id, approvedBy);
    }
    @PutMapping("/protocols/{id}/reject")
    public ProtocolResponseDTO rejectProtocol(
            @PathVariable Long id,
            @RequestParam String rejectedReason,
            @RequestParam(defaultValue = "system") String rejectedBy) {
        return anesthesiologistService.rejectProtocol(id, rejectedReason, rejectedBy);
    }
    @GetMapping("/protocols/escalation/{escalationId}")
    public List<ProtocolResponseDTO> getProtocolsByEscalation(@PathVariable Long escalationId) {
        return anesthesiologistService.getProtocolsByEscalation(escalationId);
    }
    @GetMapping("/protocols/pending-approval")
    public List<ProtocolResponseDTO> getPendingApprovalProtocols() {
        return anesthesiologistService.getPendingApprovalProtocols();
    }
    // ================= COMMENT ENDPOINTS ================= //
    @PostMapping("/protocols/{protocolId}/comments")
    public CommentResponseDTO addComment(
            @PathVariable Long protocolId,
            @Valid @RequestBody CommentRequestDTO commentRequest) {
        commentRequest.setProtocolId(protocolId);
        return anesthesiologistService.addComment(commentRequest);
    }

    @GetMapping("/protocols/{protocolId}/comments")
    public List<CommentResponseDTO> getCommentsByProtocol(@PathVariable Long protocolId) {
        return anesthesiologistService.getCommentsByProtocol(protocolId);
    }

    @DeleteMapping("/comments/{commentId}")
    public void deleteComment(
            @PathVariable Long commentId,
            @RequestParam(defaultValue = "system") String userId) {
        anesthesiologistService.deleteComment(commentId, userId);
    }
}
