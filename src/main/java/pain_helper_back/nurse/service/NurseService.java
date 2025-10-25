package pain_helper_back.nurse.service;

import pain_helper_back.common.patients.dto.*;


import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface NurseService {
    PatientDTO createPatient(PatientDTO patientDto);

    PatientDTO getPatientByMrn(String mrn);

    PatientDTO getPatientByEmail(String email);

    PatientDTO getPatientByPhoneNumber(String phoneNumber);

    List<PatientDTO> searchPatients(
            String firstName,
            String lastName,
            Boolean isActive,
            LocalDate birthDate
    );

    void deletePatient(String mrn);

    PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto);

    EmrDTO createEmr(String mrn, EmrDTO emrDto);

    EmrDTO getLastEmrByPatientMrn(String mrn);

    EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto);

    VasDTO createVAS(String mrn, VasDTO vasDto);

    VasDTO updateVAS(String mrn, VasDTO vasDto);

    void deleteVAS(String mrn);

    Optional<VasDTO> getLastVAS(String mrn);

    List<RecommendationDTO> getAllApprovedRecommendations();

    RecommendationDTO createRecommendation(String mrn);

    RecommendationDTO executeRecommendation (String mrn);


    Optional<RecommendationDTO> getLastRecommendation(String mrn);


}
