package pain_helper_back.nurse.service;

import pain_helper_back.nurse.dto.*;

import java.util.List;

public interface NurseService {
    PatientDto createPatient(PatientDto patientDto);

    PatientDto getPatientById(String personId);

    List<PatientDto> getAllPatients();

    void deletePatient(String personId);

    PatientDto updatePatient(String personId, PatientUpdateDto patientUpdateDto);

    EmrDto createEmr(String personId, EmrDto emrDto);

    EmrDto getLastEmrByPatientId(String personId);

    EmrDto updateEmr(String personId, EmrUpdateDto emrUpdateDto);

    VasDto createVAS(String personId, VasDto vasDto);

    VasDto updateVAS(String personId, VasDto vasDto);

//    void deleteVAS(String personId);

    RecommendationDto createRecommendation(String personId);
}
