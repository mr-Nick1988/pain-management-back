package pain_helper_back.anesthesiologist.service;

import pain_helper_back.anesthesiologist.dto.*;
import pain_helper_back.enums.EscalationStatus;
import pain_helper_back.enums.EscalationPriority;

import java.util.List;

public interface AnesthesiologistServiceInterface {

    // Escalation methods
    List<EscalationResponseDTO> getAllEscalations();
    List<EscalationResponseDTO> getEscalationsByStatus(EscalationStatus status);
    List<EscalationResponseDTO> getEscalationsByPriority(EscalationPriority priority);
    EscalationResponseDTO resolveEscalation(Long escalationId, String resolution, String resolvedBy);
    EscalationStatsDTO getEscalationStats();

    // Protocol methods
    ProtocolResponseDTO createProtocol(ProtocolRequestDTO protocolRequest);
    ProtocolResponseDTO approveProtocol(Long protocolId, String approvedBy);
    ProtocolResponseDTO rejectProtocol(Long protocolId, String rejectedReason, String rejectedBy);
    List<ProtocolResponseDTO> getProtocolsByEscalation(Long escalationId);
    List<ProtocolResponseDTO> getPendingApprovalProtocols();

    // Comment methods
    CommentResponseDTO addComment(CommentRequestDTO commentRequest);
    List<CommentResponseDTO> getCommentsByProtocol(Long protocolId);
    void deleteComment(Long commentId, String userId);
}
