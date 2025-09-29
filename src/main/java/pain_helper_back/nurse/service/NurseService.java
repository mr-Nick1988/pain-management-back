package pain_helper_back.nurse.service;

import pain_helper_back.nurse.dto.*;

import java.util.List;

public interface NurseService {
    PatientDTO createPatient(PatientDTO patientDto);

    PatientDTO getPatientById(String personId);

    List<PatientDTO> getAllPatients();

    void deletePatient(String personId);

    PatientDTO updatePatient(String personId, PatientUpdateDTO patientUpdateDto);

    EmrDTO createEmr(String personId, EmrDTO emrDto);

    EmrDTO getLastEmrByPatientId(String personId);

    EmrDTO updateEmr(String personId, EmrUpdateDTO emrUpdateDto);

    VasDTO createVAS(String personId, VasDTO vasDto);

    VasDTO updateVAS(String personId, VasDTO vasDto);

    void deleteVAS(String personId);

    RecommendationDTO createRecommendation(String personId);
}
