package pain_helper_back.anesthesiologist.service;


import pain_helper_back.anesthesiologist.dto.*;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.util.List;

/*
 * Сервис для работы анестезиолога с эскалациями и протоколами
 * Обновлен под новый workflow: Doctor → Anesthesiologist
 */
public interface AnesthesiologistServiceInterface {
    // ================= ESCALATIONS (Эскалации) ================= //

    List<EscalationResponseDTO> getAllEscalations();

    List<EscalationResponseDTO> getEscalationsByStatus(EscalationStatus status);

    List<EscalationResponseDTO> getEscalationsByPriority(EscalationPriority priority);

    List<EscalationResponseDTO> getActiveEscalationsOrderedByPriority();

    /*
     * Одобрение эскалации анестезиологом
     *
     * WORKFLOW:
     * 1. Находит эскалацию по ID
     * 2. Проверяет статус (должен быть PENDING или IN_PROGRESS)
     * 3. Обновляет Escalation:
     *    - status → RESOLVED
     *    - resolvedBy = anesthesiologistId
     *    - resolvedAt = now()
     *    - resolution = комментарий анестезиолога
     * 4. Обновляет связанную Recommendation:
     *    - status → APPROVED_BY_ANESTHESIOLOGIST
     *    - anesthesiologistId = anesthesiologistId
     *    - anesthesiologistActionAt = now()
     *    - anesthesiologistComment = комментарий
     *    - status → FINAL_APPROVED
     *    - finalApprovedBy = anesthesiologistId
     *    - finalApprovedAt = now()
     * 5. Сохраняет изменения
     *
     * @param escalationId ID эскалации
     * @param resolutionDTO DTO с данными разрешения (resolvedBy, resolution, comment, approved=true)
     * @return обновленная эскалация
     * @throws NotFoundException если эскалация не найдена
     * @throws IllegalStateException если эскалация уже разрешена
     */
    EscalationResponseDTO approveEscalation(Long escalationId, EscalationResolutionDTO resolutionDTO);

    /*
     * Отклонение эскалации анестезиологом
     *
     * WORKFLOW:
     * 1. Находит эскалацию по ID
     * 2. Проверяет статус (должен быть PENDING или IN_PROGRESS)
     * 3. Обновляет Escalation:
     *    - status → RESOLVED
     *    - resolvedBy = anesthesiologistId
     *    - resolvedAt = now()
     *    - resolution = причина отказа
     * 4. Обновляет связанную Recommendation:
     *    - status → REJECTED_BY_ANESTHESIOLOGIST
     *    - anesthesiologistId = anesthesiologistId
     *    - anesthesiologistActionAt = now()
     *    - anesthesiologistComment = комментарий
     * 5. Сохраняет изменения
     *
     * @param escalationId ID эскалации
     * @param resolutionDTO DTO с данными разрешения (resolvedBy, resolution, comment, approved=false)
     * @return обновленная эскалация
     * @throws NotFoundException если эскалация не найдена
     * @throws IllegalStateException если эскалация уже разрешена
     */
    EscalationResponseDTO rejectEscalation(Long escalationId, EscalationResolutionDTO resolutionDTO);

    //Получение статистики по эскалациям(количество по статусам и приоритетам)
    EscalationStatsDTO getEscalationStats();

    // ================= PROTOCOLS (Протоколы лечения) ================= //
    ProtocolResponseDTO createProtocol(ProtocolRequestDTO protocolRequest);

    ProtocolResponseDTO approveProtocol(Long protocolId, String approvedBy);

    ProtocolResponseDTO rejectProtocol(Long protocolId, String rejectedReason, String rejectedBy);

    //Получение протоколов по эскалации
    List<ProtocolResponseDTO> getProtocolsByEscalation(Long escalationId);

    // Получение протоколов, ожидающих одобрения
    List<ProtocolResponseDTO> getPendingApprovalProtocols();

    // ================= COMMENTS (Комментарии) ================= //
    CommentResponseDTO addComment(CommentRequestDTO commentRequest);

    // Получение комментариев к протоколу
    List<CommentResponseDTO> getCommentsByProtocol(Long protocolId);

    void deleteComment(Long commentId, String userId);
}
