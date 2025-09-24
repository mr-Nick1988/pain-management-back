package pain_helper_back.doctor.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.admin.repository.PersonRepository;
import pain_helper_back.doctor.dto.*;
import pain_helper_back.doctor.entity.Patient;
import pain_helper_back.doctor.entity.Recommendation;
import pain_helper_back.doctor.repository.PatientRepository;
import pain_helper_back.doctor.repository.RecommendationRepository;
import pain_helper_back.enums.RecommendationStatus;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {
    private final RecommendationRepository recommendationRepository;
    private final PatientRepository patientRepository;
    private final PersonRepository personRepository;
    private final ModelMapper modelMapper;

    @Override
    public List<RecommendationDTO> getAllRecommendations() {
        List<Recommendation> recommendations = recommendationRepository.findAllByOrderByCreatedAtDesc();
        return recommendations.stream()
                .map(recommendation -> modelMapper.map(recommendation, RecommendationDTO.class))
                .toList();
    }

    @Override
    public RecommendationDTO getRecommendationById(Long id) {
        Recommendation recommendation = recommendationRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    @Transactional
    public RecommendationDTO createRecommendation(RecommendationRequestDTO dto, String createdByLogin) {
        Person createdBy = personRepository.findByLogin(createdByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        Patient patient = patientRepository.findById(dto.getPatientId()).orElseThrow(() -> new RuntimeException("Patient not found"));

        Recommendation recommendation = new Recommendation();
        recommendation.setPatient(patient);
        recommendation.setDescription(dto.getDescription());
        recommendation.setJustification(dto.getJustification());
        recommendation.setStatus(RecommendationStatus.PENDING);
        recommendation.setCreatedBy(createdBy);
        recommendationRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    @Transactional
    public RecommendationDTO approveRecommendation(Long id, RecommendationApprovalDTO dto, String approvedByLogin) {
        Recommendation recommendation = recommendationRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        if (!recommendation.getStatus().equals(RecommendationStatus.PENDING)) {
            throw new RuntimeException("Only PENDING recommendations can be approved");
        }
        Person updatedBy = personRepository.findByLogin(approvedByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        recommendation.setStatus(RecommendationStatus.APPROVED);
        recommendation.setDoctorComment(dto.getComment());
        recommendation.setUpdatedBy(updatedBy);
        recommendationRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    @Transactional
    public RecommendationDTO rejectRecommendation(Long id, RecommendationApprovalDTO dto, String rejectedByLogin) {
        Recommendation recommendation = recommendationRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
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
        recommendationRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    @Transactional
    public RecommendationDTO deleteRecommendation(Long id, String deletedByLogin) {
        Recommendation recommendation = recommendationRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        recommendationRepository.delete(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    @Transactional
    public RecommendationDTO updateRecommendation(Long id, RecommendationRequestDTO dto, String updatedByLogin) {
        Recommendation recommendation = recommendationRepository.findById(id).orElseThrow(() -> new RuntimeException("Recommendation not found"));
        Person updatedBy = personRepository.findByLogin(updatedByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        Patient patient = patientRepository.findById(dto.getPatientId()).orElseThrow(() -> new RuntimeException("Patient not found"));
        recommendation.setPatient(patient);
        recommendation.setDescription(dto.getDescription());
        recommendation.setJustification(dto.getJustification());
        recommendation.setUpdatedBy(updatedBy);
        recommendationRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    //Patient methods
    @Override
    public List<PatientResponseDTO> getAllPatients() {
        List<Patient> patients = patientRepository.findByActiveTrueOrderByCreatedAtDesc();
        return patients.stream()
                .map(patient -> modelMapper.map(patient, PatientResponseDTO.class))
                .toList();
    }

    @Override
    public PatientResponseDTO getPatientById(Long id) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        return modelMapper.map(patient, PatientResponseDTO.class);
    }

    @Override
    @Transactional
    public PatientResponseDTO createPatient(PatientCreationDTO dto, String createdByLogin) {
        if (patientRepository.existsByEmrNumber((dto.getEmrNumber()))) {
            throw new RuntimeException("Patient with emr number " + dto.getEmrNumber() + " already exists");
        }
        Person createdBy = personRepository.findByLogin(createdByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        Patient patient = new Patient();
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setEmrNumber(dto.getEmrNumber());
        patient.setAdditionalInfo(dto.getAdditionalInfo());
        patient.setCreatedBy(createdBy);
        patientRepository.save(patient);
        return modelMapper.map(patient, PatientResponseDTO.class);
    }


    @Override
    public PatientResponseDTO updatePatient(Long id, PatientResponseDTO dto, String updatedByLogin) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        if (!patient.getEmrNumber().equals(dto.getEmrNumber()) && patientRepository.existsByEmrNumber(dto.getEmrNumber())) {
            throw new RuntimeException("Patient with emr number " + dto.getEmrNumber() + " already exists");
        }
        patient.setFirstName(dto.getFirstName());
        patient.setLastName(dto.getLastName());
        patient.setEmrNumber(dto.getEmrNumber());
        patient.setAdditionalInfo(dto.getAdditionalInfo());
        patientRepository.save(patient);
        return modelMapper.map(patient, PatientResponseDTO.class);
    }

    @Override
    @Transactional
    public PatientResponseDTO deletePatient(Long id, String deletedByLogin) {
        Patient patient = patientRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        patient.setActive(false);
        patientRepository.save(patient);
        return modelMapper.map(patient, PatientResponseDTO.class);
    }
}
