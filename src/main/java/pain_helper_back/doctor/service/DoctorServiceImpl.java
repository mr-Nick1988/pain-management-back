package pain_helper_back.doctor.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.admin.repository.PersonRepository;
import pain_helper_back.doctor.dto.*;
import pain_helper_back.doctor.entity.AuditTrail;
import pain_helper_back.doctor.entity.Patients;
import pain_helper_back.doctor.entity.Recommendation;
import pain_helper_back.doctor.repository.AuditTrailRepository;
import pain_helper_back.doctor.repository.PatientsRepository;
import pain_helper_back.doctor.repository.RecommendationsRepository;
import pain_helper_back.enums.PatientRegistrationAuditAction;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {
    private final RecommendationsRepository recommendationsRepository;
    private final PatientsRepository patientsRepository;
    private final PersonRepository personRepository;
    private final AuditTrailRepository auditTrailRepository;
    private final ModelMapper modelMapper;
    private static final Logger log = LoggerFactory.getLogger(DoctorServiceImpl.class);



    //Recommendation methods
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
    public List<PatientResponseDTO> searchPatients(String firstName, String lastName, LocalDate dateOfBirth, String insurance, String mrn) {
        List<Patients> patients = new ArrayList<>();
        // Search by MRN if provided
        if (mrn != null && !mrn.trim().isEmpty()) {
            patientsRepository.findByMrn(mrn).ifPresent(patients::add);
        }
        // Search by insurance policy if provided
        if (insurance != null && !insurance.trim().isEmpty()) {
            patients.addAll(patientsRepository.findByInsurancePolicyNumber(insurance));
        }
        // Search by name and DOB if provided
        if (firstName != null && !firstName.trim().isEmpty() &&
                lastName != null && !lastName.trim().isEmpty()) {
            if (dateOfBirth != null) {
                patientsRepository.findByFirstNameAndLastNameAndDateOfBirth(firstName, lastName, dateOfBirth)
                        .ifPresent(patients::add);
            } else {
                //Search without date of birth - find all patients with matching names
                patients.addAll(patientsRepository.findByFirstNameAndLastName(firstName, lastName));
            }
        }
        // Remove duplicates and return the list
        return patients.stream()
                .distinct()
                .map(p -> modelMapper.map(p, PatientResponseDTO.class))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PatientResponseDTO getPatientById(Long id) {
        Patients patients = patientsRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        return modelMapper.map(patients, PatientResponseDTO.class);
    }

    @Override
    public PatientResponseDTO createPatient(PatientCreationDTO dto, String createdByLogin) {
        //Find doctor, who created patient
        Person createdBy = personRepository.findByLogin(createdByLogin).orElseThrow(() -> new RuntimeException("Person not found"));
        //search by insurance policy number
        if (dto.getInsurancePolicyNumber() != null && !dto.getInsurancePolicyNumber().trim().isEmpty()) {
            Optional<Patients> existing = patientsRepository.findByFirstNameAndLastNameAndDateOfBirthAndInsurancePolicyNumber(
                    dto.getFirstName(), dto.getLastName(), dto.getDateOfBirth(), dto.getInsurancePolicyNumber());

            if (existing.isPresent()) {
                //Patient find-reregistration(back existing data)
                Patients patient = existing.get();
                patient.setUpdatedBy(createdBy);//renew, who is the last who have updated this patient
                patientsRepository.save(patient);
                //Audit trail: fixing reregistration for compliance
                AuditTrail audit = new AuditTrail();
                audit.setAction(PatientRegistrationAuditAction.PATIENT_RE_REGISTERED);//enum4ik
                audit.setPerson(createdBy);
                audit.setPid(patient.getId());// PID - id for the medical system
                auditTrailRepository.save(audit);
                return modelMapper.map(patient, PatientResponseDTO.class);
            }
        }
        //New patient - create EMR (electronic medical record)
        Patients patients = new Patients();
        patients.setFirstName(dto.getFirstName());
        patients.setLastName(dto.getLastName());
        patients.setDateOfBirth(dto.getDateOfBirth());
        patients.setGender(dto.getGender());
        patients.setInsurancePolicyNumber(dto.getInsurancePolicyNumber());
        patients.setPhoneNumber(dto.getPhoneNumber());
        patients.setEmail(dto.getEmail());
        patients.setAddress(dto.getAddress());
        patients.setAdditionalInfo(dto.getAdditionalInfo());
        patients.setCreatedBy(createdBy);
        patientsRepository.save(patients);//save to get ID (DB)

        //Generate unique MRN (medical record number) for hospital
        patients.setMrn("MRN-" + patients.getId());
        patientsRepository.save(patients);//renew with MRN
        //Audit trail: fixing registration for compliance
        AuditTrail audit = new AuditTrail();
        audit.setAction(PatientRegistrationAuditAction.PATIENT_REGISTERED);//enum
        audit.setPerson(createdBy);
        audit.setPid(patients.getId());
        auditTrailRepository.save(audit);
        return modelMapper.map(patients, PatientResponseDTO.class);
    }


    @Override
    public PatientResponseDTO updatePatient(Long id, PatientResponseDTO dto, String updatedByLogin) {
        Patients patients = patientsRepository.findById(id).orElseThrow(() -> new RuntimeException("Patient not found"));
        if (!patients.getMrn().equals(dto.getMrn()) && patientsRepository.existsByMrn(dto.getMrn())) {
            throw new RuntimeException("Patient with  medical record number " + dto.getMrn() + " already exists");
        }
        patients.setFirstName(dto.getFirstName());
        patients.setLastName(dto.getLastName());
        patients.setMrn(dto.getMrn());
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
