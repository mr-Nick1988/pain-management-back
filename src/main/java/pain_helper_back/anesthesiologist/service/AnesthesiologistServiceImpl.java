package pain_helper_back.anesthesiologist.service;


import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.anesthesiologist.dto.*;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.anesthesiologist.entity.TreatmentProtocol;
import pain_helper_back.anesthesiologist.entity.TreatmentProtocolComment;
import pain_helper_back.anesthesiologist.repository.ProtocolCommentRepository;
import pain_helper_back.anesthesiologist.repository.TreatmentEscalationRepository;
import pain_helper_back.anesthesiologist.repository.TreatmentProtocolRepository;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;
import pain_helper_back.enums.ProtocolStatus;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AnesthesiologistServiceImpl implements AnesthesiologistServiceInterface {
    private final TreatmentEscalationRepository escalationRepository;
    private final TreatmentProtocolRepository protocolRepository;
    private final ProtocolCommentRepository commentRepository;
    private final ModelMapper modelMapper;


    @Override
    @Transactional(readOnly = true)
    public List<EscalationResponseDTO> getAllEscalations() {
        return escalationRepository.findAll().stream()
                .map(escalation -> modelMapper.map(escalation, EscalationResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscalationResponseDTO> getEscalationsByStatus(EscalationStatus status) {
        return escalationRepository.findAllByEscalationStatus(status).stream()
                .map(escalation -> modelMapper.map(escalation, EscalationResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscalationResponseDTO> getEscalationsByPriority(EscalationPriority priority) {
        return escalationRepository.findByEscalationPriority(priority).stream()
                .map(escalation -> modelMapper.map(escalation, EscalationResponseDTO.class))
                .toList();
    }

    @Override
    public EscalationResponseDTO resolveEscalation(Long escalationId, String resolution, String resolvedBy) {
        Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new RuntimeException("Escalation not found with id: " + escalationId));
        escalation.setEscalationStatus(EscalationStatus.RESOLVED);
        escalation.setResolution(resolution);
        escalation.setResolvedAt(LocalDateTime.now());
        Escalation savedEscalation = escalationRepository.save(escalation);
        return modelMapper.map(savedEscalation, EscalationResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public EscalationStatsDTO getEscalationStats() {
        Long total = escalationRepository.count();
        Long pending = escalationRepository.countByEscalationStatus(EscalationStatus.PENDING);
        Long inProgress = escalationRepository.countByEscalationStatus(EscalationStatus.IN_REVIEW);
        Long resolved = escalationRepository.countByEscalationStatus(EscalationStatus.RESOLVED);
        Long high = escalationRepository.countByEscalationPriority(EscalationPriority.HIGH);
        Long medium = escalationRepository.countByEscalationPriority(EscalationPriority.MEDIUM);
        Long low = escalationRepository.countByEscalationPriority(EscalationPriority.LOW);
        return new EscalationStatsDTO(total, pending, inProgress, resolved, high, medium, low);
    }

    @Override
    public ProtocolResponseDTO createProtocol(ProtocolRequestDTO protocolRequest) {
        TreatmentProtocol protocol = modelMapper.map(protocolRequest, TreatmentProtocol.class);
        protocol.setStatus(ProtocolStatus.DRAFT);
        protocol.setVersion(1);
        TreatmentProtocol savedProtocol = protocolRepository.save(protocol);
        return modelMapper.map(savedProtocol, ProtocolResponseDTO.class);
    }

    @Override
    public ProtocolResponseDTO approveProtocol(Long protocolId, String approvedBy) {
        TreatmentProtocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new RuntimeException("Protocol not found with id: " + protocolId));

        protocol.setStatus(ProtocolStatus.APPROVED);
        protocol.setApprovedBy(approvedBy);
        protocol.setApprovedAt(LocalDateTime.now());
        TreatmentProtocol savedProtocol = protocolRepository.save(protocol);
        return modelMapper.map(savedProtocol, ProtocolResponseDTO.class);
    }

    @Override
    public ProtocolResponseDTO rejectProtocol(Long protocolId, String rejectedReason, String rejectedBy) {
        TreatmentProtocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new RuntimeException("Protocol not found with id: " + protocolId));
        protocol.setStatus(ProtocolStatus.REJECTED);
        protocol.setRejectedReason(rejectedReason);
        TreatmentProtocol savedProtocol = protocolRepository.save(protocol);
        return modelMapper.map(savedProtocol, ProtocolResponseDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProtocolResponseDTO> getProtocolsByEscalation(Long escalationId) {
        return protocolRepository.findByEscalationId(escalationId).stream()
                .map(protocol -> modelMapper.map(protocol, ProtocolResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProtocolResponseDTO> getPendingApprovalProtocols() {
        return protocolRepository.findByStatus(ProtocolStatus.PENDING_APPROVAL).stream()
                .map(protocol -> modelMapper.map(protocol, ProtocolResponseDTO.class))
                .toList();
    }

    @Override
    public CommentResponseDTO addComment(CommentRequestDTO commentRequest) {
        TreatmentProtocolComment comment = modelMapper.map(commentRequest, TreatmentProtocolComment.class);
        TreatmentProtocolComment savedComment = commentRepository.save(comment);
        return modelMapper.map(savedComment, CommentResponseDTO.class);
    }

    @Override
    public List<CommentResponseDTO> getCommentsByProtocol(Long protocolId) {
        return commentRepository.findByProtocolId(protocolId).stream()
                .map(comment -> modelMapper.map(comment, CommentResponseDTO.class))
                .toList();
    }

    @Override
    public void deleteComment(Long commentId, String userId) {
        TreatmentProtocolComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + commentId));
        if (!comment.getAuthorId().equals(userId)) {
            throw new RuntimeException("User is not authorized to delete this comment");
        }
        commentRepository.delete(comment);
    }
}
