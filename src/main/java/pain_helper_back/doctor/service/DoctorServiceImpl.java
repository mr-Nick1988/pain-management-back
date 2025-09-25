package pain_helper_back.doctor.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.admin.repository.PersonRepository;
import pain_helper_back.doctor.dto.*;
import pain_helper_back.doctor.entity.Patients;
import pain_helper_back.doctor.entity.Recommendation;
import pain_helper_back.doctor.repository.PatientsRepository;
import pain_helper_back.doctor.repository.RecommendationsRepository;
import pain_helper_back.enums.RecommendationStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {
    private final RecommendationsRepository recommendationsRepository;
    private final PatientsRepository patientsRepository;
    private final PersonRepository personRepository;
    private final ModelMapper modelMapper;

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationDTO> getAllRecommendations() {
        List<Recommendation> recommendations = recommendationsRepository.findAllByOrderByCreatedAtDesc();
        return recommendations.stream()
                .map(recommendation -> modelMapper.map(recommendation, RecommendationDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationDTO getRecommendationById(Long id) {
        Recommendation recommendation = recommendationsRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    public RecommendationDTO createRecommendation(RecommendationRequestDTO dto, String createdByLogin) {
        Person createdBy = personRepository.findByLogin(createdByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        Patients patients = patientsRepository.findById(dto.getPatientId()).orElseThrow(() -> new RuntimeException("Patient not found"));

        Recommendation recommendation = new Recommendation();
        recommendation.setPatients(patients);
        recommendation.setDescription(dto.getDescription());
        recommendation.setJustification(dto.getJustification());
        recommendation.setStatus(RecommendationStatus.PENDING);
        recommendation.setCreatedBy(createdBy);
        recommendationsRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    public RecommendationDTO approveRecommendation(Long id, RecommendationApprovalDTO dto, String approvedByLogin) {
        Recommendation recommendation = recommendationsRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        if (!recommendation.getStatus().equals(RecommendationStatus.PENDING)) {
            throw new RuntimeException("Only PENDING recommendations can be approved");
        }
        Person updatedBy = personRepository.findByLogin(approvedByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        recommendation.setStatus(RecommendationStatus.APPROVED);
        recommendation.setDoctorComment(dto.getComment());
        recommendation.setUpdatedBy(updatedBy);
        recommendationsRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    public RecommendationDTO rejectRecommendation(Long id, RecommendationApprovalDTO dto, String rejectedByLogin) {
        Recommendation recommendation = recommendationsRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        if (!recommendation.getStatus().equals(RecommendationStatus.PENDING)) {
            throw new RuntimeException("Only PENDING recommendations can be rejected");
        }
        if (dto.getRejectedReason() == null || dto.getRejectedReason().trim().isEmpty()) {
            throw new RuntimeException("Rejected reason is required");
        }
        Person updatedBy = personRepository.findByLogin(rejectedByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        recommendation.setStatus(RecommendationStatus.REJECTED);
        recommendation.setRejectionReason(dto.getRejectedReason());
        recommendation.setDoctorComment(dto.getComment());
        recommendation.setUpdatedBy(updatedBy);
        recommendationsRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    public RecommendationDTO deleteRecommendation(Long id, String deletedByLogin) {
        Recommendation recommendation = recommendationsRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        recommendationsRepository.delete(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    public RecommendationDTO updateRecommendation(Long id, RecommendationRequestDTO dto, String updatedByLogin) {
        Recommendation recommendation = recommendationsRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        Person updatedBy = personRepository.findByLogin(updatedByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        Patients patients = patientsRepository.findById(dto.getPatientId()).orElseThrow(() -> new RuntimeException("Patient not found"));
        recommendation.setPatients(patients);
        recommendation.setDescription(dto.getDescription());
        recommendation.setJustification(dto.getJustification());
        recommendation.setUpdatedBy(updatedBy);
        recommendationsRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    //Patient methods
    @Override
    @Transactional(readOnly = true)
    public List<PatientResponseDTO> getAllPatients() {
        List<Patients> patients = patientsRepository.findByActiveTrueOrderByCreatedAtDesc();
        return patients.stream()
                .map(patient -> modelMapper.map(patient, PatientResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(Long id) {
        Patients patients = patientsRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        return modelMapper.map(patients, PatientResponseDTO.class);
    }

    @Override
    @Transactional
    public PatientResponseDTO createPatient(PatientCreationDTO dto, String createdByLogin) {
        if (patientsRepository.existsByEmrNumber((dto.getEmrNumber()))) {
            throw new RuntimeException("Patient with emr number " + dto.getEmrNumber() + " already exists");
        }
        Person createdBy = personRepository.findByLogin(createdByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        Patients patients = new Patients();
        patients.setFirstName(dto.getFirstName());
        patients.setLastName(dto.getLastName());
        patients.setEmrNumber(dto.getEmrNumber());
        patients.setAdditionalInfo(dto.getAdditionalInfo());
        patients.setCreatedBy(createdBy);
        patientsRepository.save(patients);
        return modelMapper.map(patients, PatientResponseDTO.class);
    }


    @Override
    public PatientResponseDTO updatePatient(Long id, PatientResponseDTO dto, String updatedByLogin) {
        Patients patients = patientsRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        if (!patients.getEmrNumber().equals(dto.getEmrNumber()) && patientsRepository.existsByEmrNumber(dto.getEmrNumber())) {
            throw new RuntimeException("Patient with emr number " + dto.getEmrNumber() + " already exists");
        }
        patients.setFirstName(dto.getFirstName());
        patients.setLastName(dto.getLastName());
        patients.setEmrNumber(dto.getEmrNumber());
        patients.setAdditionalInfo(dto.getAdditionalInfo());
        patientsRepository.save(patients);
        return modelMapper.map(patients, PatientResponseDTO.class);
    }

    @Override
    public PatientResponseDTO deletePatient(Long id, String deletedByLogin) {
        Patients patients = patientsRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        patients.setActive(false);
        patientsRepository.save(patients);
        return modelMapper.map(patients, PatientResponseDTO.class);
    }
}
