package pain_helper_back.doctor.service;


import pain_helper_back.common.patients.dto.*;
import pain_helper_back.doctor.dto.*;

import java.time.LocalDate;
import java.util.List;

public interface DoctorService {

    //======================= Same with Nurse Methods =======================
    PatientDTO createPatient(PatientDTO patientDto);
    PatientDTO getPatientByMrn(String mrn);
    PatientDTO getPatientByEmail(String email);
    PatientDTO getPatientByPhoneNumber(String phoneNumber);

    List<PatientDTO> searchPatients(
            String firstName,
            String lastName,
            Boolean isActive,
            LocalDate birthDate,
            String gender,
            String insurancePolicyNumber,
            String address,
            String phoneNumber,
            String email
    );

    void deletePatient(String mrn);


    PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto);

    EmrDTO createEmr(String mrn, EmrDTO emrDto);

    EmrDTO getLastEmrByPatientMrn(String mrn);

    EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto);
//===================================================================================================//

    List<EmrDTO> getAllEmrByPatientMrn(String mrn);   // дополнительный функционал (можно добавить ещё кнопку)



    //recommendation methods
    List<RecommendationWithVasDTO> getAllPendingRecommendations();  // Именно тут потребуется открыть опциональное поле из RecommendationDTO private String patientMrn;

    RecommendationWithVasDTO getLastRecommendationByMrn(String mrn);

    RecommendationDTO approveRecommendation(String mrn, RecommendationApprovalRejectionDTO dto);

    RecommendationDTO rejectRecommendation(String mrn, RecommendationApprovalRejectionDTO dto);












}
