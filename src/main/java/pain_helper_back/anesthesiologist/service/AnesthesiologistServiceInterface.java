package pain_helper_back.anesthesiologist.service;


import jakarta.validation.Valid;
import pain_helper_back.anesthesiologist.dto.*;
import pain_helper_back.common.patients.dto.RecommendationDTO;
import pain_helper_back.common.patients.dto.RecommendationWithVasDTO;
import pain_helper_back.common.patients.dto.RecommendationApprovalRejectionDTO;

import java.util.List;

/*
 * Сервис для работы анестезиолога с эскалациями и протоколами
 * Обновлен под новый workflow: Doctor → Anesthesiologist
 */
public interface AnesthesiologistServiceInterface {
    // ================= ESCALATIONS (Эскалации) ================= //

    List<RecommendationWithVasDTO> getAllEscalations();


    RecommendationDTO approveEscalation(Long recommendationId, RecommendationApprovalRejectionDTO resolutionDTO);

    RecommendationDTO rejectEscalation(Long recommendationId, RecommendationApprovalRejectionDTO resolutionDTO);


    // ================= PROTOCOLS (Протоколы лечения) ================= //

    RecommendationDTO createRecommendationAfterRejection( AnesthesiologistRecommendationCreateDTO dto);

    RecommendationDTO updateRecommendation(Long id, AnesthesiologistRecommendationUpdateDTO dto);
}
