package pain_helper_back.doctor.service;

import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.exceptions.EntityExistsException;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.*;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.common.patients.repository.VasRepository;
import pain_helper_back.enums.PatientsGenders;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Implementation of DoctorService interface.
 * Handles all doctor-related operations: patients, EMR, and recommendations.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DoctorServiceImpl implements DoctorService {

    private final PatientRepository patientRepository;
    private final EmrRepository emrRepository;
    private final VasRepository vasRepository;
    private final RecommendationRepository recommendationRepository;
    private final ModelMapper modelMapper;

    /**
     * Helper method to find patient by MRN or throw NotFoundException
     */
    private Patient findPatientOrThrow(String mrn) {
        return patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with MRN " + mrn + " not found"));
    }

    // ================= PATIENTS ================= //

    @Override
    @Transactional
    public PatientDTO createPatient(PatientDTO patientDto) {
        log.info("Creating new patient: firstName={}, lastName={}", 
                patientDto.getFirstName(), patientDto.getLastName());

        // Validate unique constraints
        if (patientDto.getEmail() != null && patientRepository.existsByEmail(patientDto.getEmail())) {
            throw new EntityExistsException("Patient with this email already exists");
        }
        if (patientDto.getPhoneNumber() != null && patientRepository.existsByPhoneNumber(patientDto.getPhoneNumber())) {
            throw new EntityExistsException("Patient with this phone number already exists");
        }

        // Map DTO to Entity
        Patient patient = modelMapper.map(patientDto, Patient.class);
        patient.setIsActive(true); // New patients are active by default

        // Save to get generated ID
        patientRepository.save(patient);

        // Generate MRN based on ID
        String mrn = String.format("%06d", patient.getId());
        patient.setMrn(mrn);

        // Save again with MRN
        patientRepository.save(patient);

        log.info("Patient created successfully: mrn={}", mrn);
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByMrn(String mrn) {
        log.debug("Fetching patient by MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByEmail(String email) {
        log.debug("Fetching patient by email: {}", email);
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Patient with email " + email + " not found"));
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByPhoneNumber(String phoneNumber) {
        log.debug("Fetching patient by phone number: {}", phoneNumber);
        Patient patient = patientRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Patient with phone number " + phoneNumber + " not found"));
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDTO> searchPatients(
            String firstName,
            String lastName,
            Boolean isActive,
            LocalDate birthDate,
            String gender,
            String insurancePolicyNumber,
            String address,
            String phoneNumber,
            String email
    ) {
        log.debug("Searching patients with criteria: firstName={}, lastName={}, isActive={}, birthDate={}, gender={}, insurance={}, address={}, phone={}, email={}",
                firstName, lastName, isActive, birthDate, gender, insurancePolicyNumber, address, phoneNumber, email);

        // Build dynamic specification based on provided parameters
        Specification<Patient> spec = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            // firstName - partial match, case insensitive
            if (firstName != null && !firstName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("firstName")),
                        "%" + firstName.toLowerCase() + "%"
                ));
            }

            // lastName - partial match, case insensitive
            if (lastName != null && !lastName.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("lastName")),
                        "%" + lastName.toLowerCase() + "%"
                ));
            }

            // isActive - exact match
            if (isActive != null) {
                predicates.add(criteriaBuilder.equal(root.get("isActive"), isActive));
            }

            // birthDate - exact match
            if (birthDate != null) {
                predicates.add(criteriaBuilder.equal(root.get("dateOfBirth"), birthDate));
            }

            // gender - exact match (convert String to enum)
            if (gender != null && !gender.isBlank()) {
                try {
                    PatientsGenders genderEnum = PatientsGenders.valueOf(gender.toUpperCase());
                    predicates.add(criteriaBuilder.equal(root.get("gender"), genderEnum));
                } catch (IllegalArgumentException e) {
                    log.warn("Invalid gender value: {}", gender);
                }
            }

            // insurancePolicyNumber - partial match
            if (insurancePolicyNumber != null && !insurancePolicyNumber.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        root.get("insurancePolicyNumber"),
                        "%" + insurancePolicyNumber + "%"
                ));
            }

            // address - partial match, case insensitive
            if (address != null && !address.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("address")),
                        "%" + address.toLowerCase() + "%"
                ));
            }

            // phoneNumber - partial match
            if (phoneNumber != null && !phoneNumber.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        root.get("phoneNumber"),
                        "%" + phoneNumber + "%"
                ));
            }

            // email - partial match, case insensitive
            if (email != null && !email.isBlank()) {
                predicates.add(criteriaBuilder.like(
                        criteriaBuilder.lower(root.get("email")),
                        "%" + email.toLowerCase() + "%"
                ));
            }

            // Combine all predicates with AND
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };

        // Execute search
        List<Patient> patients = patientRepository.findAll(spec);
        log.debug("Found {} patients matching criteria", patients.size());

        // Map to DTOs
        return patients.stream()
                .map(patient -> modelMapper.map(patient, PatientDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void deletePatient(String mrn) {
        log.info("Deleting patient with MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);
        patientRepository.deleteByMrn(mrn);
        log.info("Patient deleted successfully: mrn={}", mrn);
    }

    @Override
    @Transactional
    public PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto) {
        log.info("Updating patient with MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);

        // Update only non-null fields
        if (patientUpdateDto.getFirstName() != null) {
            patient.setFirstName(patientUpdateDto.getFirstName());
        }
        if (patientUpdateDto.getLastName() != null) {
            patient.setLastName(patientUpdateDto.getLastName());
        }
        if (patientUpdateDto.getGender() != null) {
            patient.setGender(patientUpdateDto.getGender());
        }
        if (patientUpdateDto.getInsurancePolicyNumber() != null) {
            patient.setInsurancePolicyNumber(patientUpdateDto.getInsurancePolicyNumber());
        }
        if (patientUpdateDto.getPhoneNumber() != null) {
            patient.setPhoneNumber(patientUpdateDto.getPhoneNumber());
        }
        if (patientUpdateDto.getEmail() != null) {
            patient.setEmail(patientUpdateDto.getEmail());
        }
        if (patientUpdateDto.getAddress() != null) {
            patient.setAddress(patientUpdateDto.getAddress());
        }
        if (patientUpdateDto.getAdditionalInfo() != null) {
            patient.setAdditionalInfo(patientUpdateDto.getAdditionalInfo());
        }
        if (patientUpdateDto.getIsActive() != null) {
            patient.setIsActive(patientUpdateDto.getIsActive());
        }

        // Save changes (transaction will handle this)
        patientRepository.save(patient);
        log.info("Patient updated successfully: mrn={}", mrn);

        return modelMapper.map(patient, PatientDTO.class);
    }

    // ================= EMR (Electronic Medical Records) ================= //

    @Override
    @Transactional
    public EmrDTO createEmr(String mrn, EmrDTO emrDto) {
        log.info("Creating EMR for patient with MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);

        // Map DTO to Entity
        Emr emr = modelMapper.map(emrDto, Emr.class);
        emr.setPatient(patient);

        // Set bidirectional relationship for diagnoses
        if (emr.getDiagnoses() != null && !emr.getDiagnoses().isEmpty()) {
            emr.getDiagnoses().forEach(diagnosis -> diagnosis.setEmr(emr));
        }

        // Add EMR to patient collection
        patient.getEmr().add(emr);
        emrRepository.save(emr);

        log.info("EMR created successfully for patient: mrn={}", mrn);
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public EmrDTO getLastEmrByPatientMrn(String mrn) {
        log.debug("Fetching last EMR for patient with MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);

        if (patient.getEmr().isEmpty()) {
            throw new NotFoundException("No EMR records found for patient with MRN " + mrn);
        }

        Emr lastEmr = patient.getEmr().getLast();
        return modelMapper.map(lastEmr, EmrDTO.class);
    }

    @Override
    @Transactional
    public EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto) {
        log.info("Updating last EMR for patient with MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);

        if (patient.getEmr().isEmpty()) {
            throw new NotFoundException("No EMR records found for patient with MRN " + mrn);
        }

        Emr emr = patient.getEmr().getLast();

        // Update only non-null fields
        if (emrUpdateDto.getHeight() != null) {
            emr.setHeight(emrUpdateDto.getHeight());
        }
        if (emrUpdateDto.getWeight() != null) {
            emr.setWeight(emrUpdateDto.getWeight());
        }
        if (emrUpdateDto.getGfr() != null) {
            emr.setGfr(emrUpdateDto.getGfr());
        }
        if (emrUpdateDto.getChildPughScore() != null) {
            emr.setChildPughScore(emrUpdateDto.getChildPughScore());
        }
        if (emrUpdateDto.getPlt() != null) {
            emr.setPlt(emrUpdateDto.getPlt());
        }
        if (emrUpdateDto.getWbc() != null) {
            emr.setWbc(emrUpdateDto.getWbc());
        }
        if (emrUpdateDto.getSat() != null) {
            emr.setSat(emrUpdateDto.getSat());
        }
        if (emrUpdateDto.getSodium() != null) {
            emr.setSodium(emrUpdateDto.getSodium());
        }
        if (emrUpdateDto.getSensitivities() != null) {
            emr.setSensitivities(emrUpdateDto.getSensitivities());
        }
        if (emrUpdateDto.getDiagnoses() != null) {
            // Clear old diagnoses
            emr.getDiagnoses().clear();

            // Add new diagnoses with bidirectional relationship
            emrUpdateDto.getDiagnoses().forEach(diagnosisDto -> {
                Diagnosis diagnosis = modelMapper.map(diagnosisDto, Diagnosis.class);
                diagnosis.setEmr(emr);
                emr.getDiagnoses().add(diagnosis);
            });
        }

        emrRepository.save(emr);
        log.info("EMR updated successfully for patient: mrn={}", mrn);

        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmrDTO> getAllEmrByPatientMrn(String mrn) {
        log.debug("Fetching all EMR records for patient with MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);

        return patient.getEmr().stream()
                .map(emr -> modelMapper.map(emr, EmrDTO.class))
                .collect(Collectors.toList());
    }

    // ================= RECOMMENDATIONS ================= //

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationWithVasDTO> getAllPendingRecommendations() {
        log.debug("Fetching all pending recommendations");

        // Fetch all recommendations with PENDING status
        List<Recommendation> recommendations = recommendationRepository.findByStatus(RecommendationStatus.PENDING);

        // Map to RecommendationWithVasDTO
        return recommendations.stream().map(recommendation -> {
            RecommendationWithVasDTO dto = new RecommendationWithVasDTO();

            // Map recommendation
            RecommendationDTO recommendationDTO = modelMapper.map(recommendation, RecommendationDTO.class);
            recommendationDTO.setPatientMrn(recommendation.getPatient().getMrn());
            dto.setRecommendation(recommendationDTO);

            // Get last VAS for this patient
            Patient patient = recommendation.getPatient();
            if (!patient.getVas().isEmpty()) {
                Vas lastVas = patient.getVas().getLast();
                VasDTO vasDTO = modelMapper.map(lastVas, VasDTO.class);
                vasDTO.setPatientMrn(patient.getMrn());
                dto.setVas(vasDTO);
            }

            dto.setPatientMrn(patient.getMrn());
            return dto;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationWithVasDTO getLastRecommendationByMrn(String mrn) {
        log.debug("Fetching last recommendation for patient with MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);

        if (patient.getRecommendations().isEmpty()) {
            throw new NotFoundException("No recommendations found for patient with MRN " + mrn);
        }

        Recommendation lastRecommendation = patient.getRecommendations().getLast();

        // Build RecommendationWithVasDTO
        RecommendationWithVasDTO dto = new RecommendationWithVasDTO();

        // Map recommendation
        RecommendationDTO recommendationDTO = modelMapper.map(lastRecommendation, RecommendationDTO.class);
        recommendationDTO.setPatientMrn(mrn);
        dto.setRecommendation(recommendationDTO);

        // Get last VAS
        if (!patient.getVas().isEmpty()) {
            Vas lastVas = patient.getVas().getLast();
            VasDTO vasDTO = modelMapper.map(lastVas, VasDTO.class);
            vasDTO.setPatientMrn(mrn);
            dto.setVas(vasDTO);
        }

        dto.setPatientMrn(mrn);
        return dto;
    }

    @Override
    @Transactional
    public RecommendationDTO approveRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto) {
        log.info("Approving recommendation: id={}", recommendationId);

        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation with ID " + recommendationId + " not found"));

        // Check if recommendation is in PENDING status
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING recommendations can be approved. Current status: " + recommendation.getStatus());
        }

        // Update recommendation status and metadata
        recommendation.setStatus(RecommendationStatus.APPROVED);
        recommendation.setDoctorComment(dto.getComment());
        recommendation.setDoctorActionAt(LocalDateTime.now());
        // TODO: Set doctorId from SecurityContextHolder when authentication is implemented
        recommendation.setDoctorId("doctor_temp"); // Temporary placeholder

        // Save changes
        recommendationRepository.save(recommendation);

        log.info("Recommendation approved successfully: id={}, status={}", recommendationId, recommendation.getStatus());

        RecommendationDTO resultDto = modelMapper.map(recommendation, RecommendationDTO.class);
        resultDto.setPatientMrn(recommendation.getPatient().getMrn());
        return resultDto;
    }

    @Override
    @Transactional
    public RecommendationDTO rejectRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto) {
        log.info("Rejecting recommendation: id={}", recommendationId);

        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation with ID " + recommendationId + " not found"));

        // Check if recommendation is in PENDING status
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Only PENDING recommendations can be rejected. Current status: " + recommendation.getStatus());
        }

        // Update recommendation status and metadata
        recommendation.setStatus(RecommendationStatus.REJECTED);
        recommendation.setRejectedReason(dto.getRejectedReason());
        recommendation.setDoctorComment(dto.getComment());
        recommendation.setDoctorActionAt(LocalDateTime.now());
        // TODO: Set doctorId from SecurityContextHolder when authentication is implemented
        recommendation.setDoctorId("doctor_temp"); // Temporary placeholder

        // Save changes
        recommendationRepository.save(recommendation);

        log.info("Recommendation rejected successfully: id={}, status={}, reason={}", 
                recommendationId, recommendation.getStatus(), dto.getRejectedReason());

        RecommendationDTO resultDto = modelMapper.map(recommendation, RecommendationDTO.class);
        resultDto.setPatientMrn(recommendation.getPatient().getMrn());
        return resultDto;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationWithVasDTO> getRecommendationsWithVasByPatientMrn(String mrn) {
        log.debug("Fetching all recommendations with VAS history for patient with MRN: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);

        List<Recommendation> recommendations = patient.getRecommendations();
        List<Vas> vasHistory = patient.getVas();

        // Map each recommendation with its corresponding VAS
        return recommendations.stream().map(recommendation -> {
            RecommendationWithVasDTO dto = new RecommendationWithVasDTO();

            // Map recommendation
            RecommendationDTO recommendationDTO = modelMapper.map(recommendation, RecommendationDTO.class);
            recommendationDTO.setPatientMrn(mrn);
            dto.setRecommendation(recommendationDTO);

            // Try to find VAS created around the same time as recommendation
            // (using simple logic: VAS created before or around recommendation creation time)
            if (!vasHistory.isEmpty()) {
                LocalDateTime recommendationCreatedAt = recommendation.getCreatedAt();
                Vas correspondingVas = vasHistory.stream()
                        .filter(vas -> vas.getCreatedAt().isBefore(recommendationCreatedAt) 
                                || vas.getCreatedAt().isEqual(recommendationCreatedAt))
                        .reduce((first, second) -> second) // Get last matching VAS
                        .orElse(vasHistory.getLast()); // Fallback to last VAS

                VasDTO vasDTO = modelMapper.map(correspondingVas, VasDTO.class);
                vasDTO.setPatientMrn(mrn);
                dto.setVas(vasDTO);
            }

            dto.setPatientMrn(mrn);
            return dto;
        }).collect(Collectors.toList());
    }
}
