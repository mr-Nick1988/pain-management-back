package pain_helper_back.anesthesiologist.service;


import pain_helper_back.common.patients.dto.*;
import pain_helper_back.anesthesiologist.dto.AnesthesiologistRecommendationCreateDTO;
import pain_helper_back.anesthesiologist.dto.AnesthesiologistRecommendationUpdateDTO;

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


    // ================= PATIENT & EMR ACCESS ================= //
    EmrDTO getLastEmrByPatientMrn(String mrn);
    PatientDTO getPatientByMrn(String mrn);

    List<RecommendationWithVasDTO> getRejectedRecommendations();

    List<RecommendationWithVasDTO> getRecommendationsWithVasByPatientMrn(String mrn);
}
