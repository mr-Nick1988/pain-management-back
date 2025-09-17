package pain_helper_back.doctor.service;


import pain_helper_back.doctor.dto.*;

import java.util.List;

public interface DoctorService {
    //recommendation methods
    List<RecommendationDTO> getAllRecommendations();

    RecommendationDTO getRecommendationById(Long id);

    RecommendationDTO createRecommendation(RecommendationRequestDTO dto, String createdByLogin);

    RecommendationDTO approveRecommendation(Long id, RecommendationApprovalDTO dto, String approvedByLogin);

    RecommendationDTO rejectRecommendation(Long id, RecommendationApprovalDTO dto, String rejectedByLogin);

    RecommendationDTO deleteRecommendation(Long id, String deletedByLogin);

    RecommendationDTO updateRecommendation(Long id, RecommendationRequestDTO dto, String updatedByLogin);

    //patient methods
    List<PatientResponseDTO> getAllPatients();

    PatientResponseDTO getPatientById(Long id);

    PatientResponseDTO createPatient(PatientCreationDTO dto, String createdByLogin);

    PatientResponseDTO updatePatient(Long id, PatientResponseDTO dto, String updatedByLogin);

    PatientResponseDTO deletePatient(Long id, String deletedByLogin);


}
