package pain_helper_back.anesthesiologist.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.anesthesiologist.dto.*;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.anesthesiologist.entity.TreatmentProtocolComment;
import pain_helper_back.anesthesiologist.entity.TreatmentsProtocol;
import pain_helper_back.anesthesiologist.repository.ProtocolCommentRepository;
import pain_helper_back.anesthesiologist.repository.TreatmentEscalationRepository;
import pain_helper_back.anesthesiologist.repository.TreatmentsProtocolRepository;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;
import pain_helper_back.enums.ProtocolStatus;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AnesthesiologistServiceImpl implements AnesthesiologistServiceInterface {
    private final TreatmentEscalationRepository escalationRepository;
    private final TreatmentsProtocolRepository protocolRepository;
    private final ProtocolCommentRepository commentRepository;
    private final RecommendationRepository recommendationRepository;
    private final ModelMapper modelMapper;

    // ================= ESCALATIONS ================= //
    @Override
    @Transactional(readOnly = true)
    public List<EscalationResponseDTO> getAllEscalations() {
        log.info("Getting all escalations");
        return escalationRepository.findAll().stream()
                .map(escalation -> modelMapper.map(escalation, EscalationResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscalationResponseDTO> getEscalationsByStatus(EscalationStatus status) {
        log.info("Getting escalations by status: {}", status);
        return escalationRepository.findByStatus(status).stream()
                .map(escalation -> modelMapper.map(escalation, EscalationResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<EscalationResponseDTO> getEscalationsByPriority(EscalationPriority priority) {
        log.info("Getting escalations by priority: {}", priority);
        return escalationRepository.findByPriority(priority).stream()
                .map(escalation -> modelMapper.map(escalation, EscalationResponseDTO.class))
                .toList();
    }


    @Override
    @Transactional(readOnly = true)
    public List<EscalationResponseDTO> getActiveEscalationsOrderedByPriority() {
        log.info("Getting active escalations ordered by priority");
        return escalationRepository.findActiveEscalationsOrderedByPriorityAndDate().stream()
                .map(escalation -> modelMapper.map(escalation, EscalationResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public EscalationStatsDTO getEscalationStats() {
        Long total = escalationRepository.count();
        Long pending = escalationRepository.countByStatus(EscalationStatus.PENDING);
        Long inProgress = escalationRepository.countByStatus(EscalationStatus.IN_REVIEW);
        Long resolved = escalationRepository.countByStatus(EscalationStatus.RESOLVED);
        Long high = escalationRepository.countByPriority(EscalationPriority.HIGH);
        Long medium = escalationRepository.countByPriority(EscalationPriority.MEDIUM);
        Long low = escalationRepository.countByPriority(EscalationPriority.LOW);
        return new EscalationStatsDTO(total, pending, inProgress, resolved, high, medium, low);
    }

    @Override
    public EscalationResponseDTO approveEscalation(Long escalationId, EscalationResolutionDTO resolutionDTO) {
        log.info("Approving escalation: id={}, resolvedBy={}", escalationId, resolutionDTO.getResolvedBy());
        // 1. Найти эскалацию
        Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new RuntimeException("Escalation not found with id: " + escalationId));
        //2. Проверить статус
        if (escalation.getStatus() == EscalationStatus.RESOLVED || escalation.getStatus() == EscalationStatus.CANCELLED) {
            throw new IllegalStateException("Escalation is already resolved or cancelled");
        }
        // 3. Обновить эскалацию
        escalation.setStatus(EscalationStatus.RESOLVED);
        escalation.setResolvedBy(resolutionDTO.getResolvedBy());
        escalation.setResolvedAt(LocalDateTime.now());
        escalation.setResolution(resolutionDTO.getResolution());
        // 4. Обновить связанную рекомендацию
        Recommendation recommendation = escalation.getRecommendation();
        // 4.1. Статус → APPROVED_BY_ANESTHESIOLOGIST
        recommendation.setStatus(RecommendationStatus.APPROVED_BY_ANESTHESIOLOGIST);
        recommendation.setAnesthesiologistId(resolutionDTO.getResolvedBy());
        recommendation.setAnesthesiologistActionAt(LocalDateTime.now());
        recommendation.setAnesthesiologistComment(resolutionDTO.getComment());
        // 4.2. Финальное одобрение
        recommendation.setStatus(RecommendationStatus.FINAL_APPROVED);
        recommendation.setFinalApprovedBy(resolutionDTO.getResolvedBy());
        recommendation.setFinalApprovalAt(LocalDateTime.now());
        // 4.3. Добавить комментарий в список
        if (resolutionDTO.getComment() != null && !resolutionDTO.getComment().isBlank()) {
            recommendation.getComments().add("Anesthesiologist: " + resolutionDTO.getComment());
        }
        // 5. Сохранить
        recommendationRepository.save(recommendation);
        Escalation savedEscalation = escalationRepository.save(escalation);
        log.info("Escalation approved: id={}, recommendationId={}, status={}",
                savedEscalation.getId(), recommendation.getId(), recommendation.getStatus());
        return modelMapper.map(savedEscalation, EscalationResponseDTO.class);
    }

    @Override
    public EscalationResponseDTO rejectEscalation(Long escalationId, EscalationResolutionDTO resolutionDTO) {
        log.info("Rejecting escalation: id={}, resolvedBy={}", escalationId, resolutionDTO.getResolvedBy());
        // 1. Найти эскалацию
        Escalation escalation = escalationRepository.findById(escalationId)
                .orElseThrow(() -> new NotFoundException("Escalation not found with id: " + escalationId));
        // 2. Проверить статус
        if (escalation.getStatus() == EscalationStatus.RESOLVED || escalation.getStatus() == EscalationStatus.CANCELLED) {
            throw new IllegalStateException("Escalation is already resolved or cancelled");
        }
        // 3. Обновить эскалацию
        escalation.setStatus(EscalationStatus.RESOLVED);
        escalation.setResolvedBy(resolutionDTO.getResolvedBy());
        escalation.setResolvedAt(LocalDateTime.now());
        escalation.setResolution(resolutionDTO.getResolution());
        // 4. Обновить связанную рекомендацию
        Recommendation recommendation = escalation.getRecommendation();
        recommendation.setStatus(RecommendationStatus.REJECTED_BY_ANESTHESIOLOGIST);
        recommendation.setAnesthesiologistId(resolutionDTO.getResolvedBy());
        recommendation.setAnesthesiologistActionAt(LocalDateTime.now());
        recommendation.setAnesthesiologistComment(resolutionDTO.getComment());
        // Добавить комментарий в список
        if (resolutionDTO.getComment() != null && !resolutionDTO.getComment().isBlank()) {
            recommendation.getComments().add("Anesthesiologist (REJECTED): " + resolutionDTO.getComment());
        }
        // 5. Сохранить
        recommendationRepository.save(recommendation);
        Escalation savedEscalation = escalationRepository.save(escalation);

        log.info("Escalation rejected: id={}, recommendationId={}, status={}",
                savedEscalation.getId(), recommendation.getId(), recommendation.getStatus());
        return modelMapper.map(savedEscalation, EscalationResponseDTO.class);
    }
    // ================= PROTOCOLS ================= //

    @Override
    public ProtocolResponseDTO createProtocol(ProtocolRequestDTO protocolRequest) {
        log.info("Creating protocol for escalation: {}", protocolRequest.getEscalationId());
        TreatmentsProtocol protocol = modelMapper.map(protocolRequest, TreatmentsProtocol.class);
        protocol.setStatus(ProtocolStatus.DRAFT);
        protocol.setVersion(1);
        TreatmentsProtocol savedProtocol = protocolRepository.save(protocol);
        log.info("Protocol created: id={}", savedProtocol.getId());
        return modelMapper.map(savedProtocol, ProtocolResponseDTO.class);
    }

    @Override
    public ProtocolResponseDTO approveProtocol(Long protocolId, String approvedBy) {
        log.info("Approving protocol: id={}, approvedBy={}", protocolId, approvedBy);
        TreatmentsProtocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new RuntimeException("Protocol not found with id: " + protocolId));
        protocol.setStatus(ProtocolStatus.APPROVED);
        protocol.setApprovedBy(approvedBy);
        protocol.setApprovedAt(LocalDateTime.now());
        TreatmentsProtocol savedProtocol = protocolRepository.save(protocol);
        return modelMapper.map(savedProtocol, ProtocolResponseDTO.class);
    }

    @Override
    public ProtocolResponseDTO rejectProtocol(Long protocolId, String rejectedReason, String rejectedBy) {
        log.info("Rejecting protocol: id={}, rejectedBy={}", protocolId, rejectedBy);
        TreatmentsProtocol protocol = protocolRepository.findById(protocolId)
                .orElseThrow(() -> new RuntimeException("Protocol not found with id: " + protocolId));
        protocol.setStatus(ProtocolStatus.REJECTED);
        protocol.setRejectedReason(rejectedReason);
        TreatmentsProtocol savedProtocol = protocolRepository.save(protocol);
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

    // ================= COMMENTS ================= //
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
