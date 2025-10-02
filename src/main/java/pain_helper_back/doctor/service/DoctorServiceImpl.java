package pain_helper_back.doctor.service;

import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.exceptions.EntityExistsException;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.doctor.dto.*;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class DoctorServiceImpl implements DoctorService {
    private final RecommendationRepository recommendationRepository;
    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;


    private Patient findPatientOrThrow(String mrn) {
        return patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with this " + mrn + " not found"));
    }


    @Override
    public PatientDTO createPatient(PatientDTO patientDto) {
        if (patientDto.getEmail() != null && patientRepository.existsByEmail(patientDto.getEmail())) {
            throw new EntityExistsException("Patient with this email already exists");
        }
        if (patientRepository.existsByPhoneNumber(patientDto.getPhoneNumber())) {
            throw new EntityExistsException("Patient with this phone number already exists");
        }
        Patient patient = modelMapper.map(patientDto, Patient.class);
        patientRepository.save(patient);
        String mrn = String.format("%06d", patient.getId());
        patient.setMrn(mrn);
        patientRepository.save(patient);
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByMrn(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByEmail(String email) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Patient with this email not found"));
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    public PatientDTO getPatientByPhoneNumber(String phoneNumber) {
        Patient patient = patientRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Patient with this phone number not found"));
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<PatientDTO> searchPatients(String firstName, String lastName, Boolean isActive, LocalDate birthDate) {
        if (firstName != null && lastName != null) {
            List<Patient> patients = patientRepository.getPatientsByFirstNameAndLastName(firstName, lastName);
            return patients.stream().map(patient -> modelMapper.map(patient, PatientDTO.class)).collect(Collectors.toList());
        }
        if (isActive != null) {
            List<Patient> patients = patientRepository.findByIsActive(isActive);
            return patients.stream().map(p -> modelMapper.map(p, PatientDTO.class)).collect(Collectors.toList());
        }
        if (birthDate != null) {
            List<Patient> patients = patientRepository.findByDateOfBirth(birthDate);
            return patients.stream().map(p -> modelMapper.map(p, PatientDTO.class)).collect(Collectors.toList());
        } else {
            List<Patient> patients = patientRepository.findAll();
            return patients.stream()
                    .map(patient -> modelMapper.map(patient, PatientDTO.class))
                    .collect(Collectors.toList());
        }
    }


    @Override
    public void deletePatient(String mrn) {
        patientRepository.deleteByMrn(mrn);
    }

    @Override
    public PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto) {
        Patient patient = findPatientOrThrow(mrn);

        if (patientUpdateDto.getFirstName() != null) patient.setFirstName(patientUpdateDto.getFirstName());
        if (patientUpdateDto.getLastName() != null) patient.setLastName(patientUpdateDto.getLastName());
        if (patientUpdateDto.getGender() != null) patient.setGender(patientUpdateDto.getGender());
        if (patientUpdateDto.getInsurancePolicyNumber() != null)
            patient.setInsurancePolicyNumber(patientUpdateDto.getInsurancePolicyNumber());
        if (patientUpdateDto.getPhoneNumber() != null) patient.setPhoneNumber(patientUpdateDto.getPhoneNumber());
        if (patientUpdateDto.getEmail() != null) patient.setEmail(patientUpdateDto.getEmail());
        if (patientUpdateDto.getAddress() != null) patient.setAddress(patientUpdateDto.getAddress());
        if (patientUpdateDto.getAdditionalInfo() != null)
            patient.setAdditionalInfo(patientUpdateDto.getAdditionalInfo());

        if (patientUpdateDto.getIsActive() != null) {
            patient.setIsActive(patientUpdateDto.getIsActive());
        }
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    public EmrDTO createEmr(String mrn, EmrDTO emrDto) {
        Patient patient = findPatientOrThrow(mrn);
        Emr emr = modelMapper.map(emrDto, Emr.class);
        emr.setPatient(patient);
        patient.getEmr().add(emr);
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public EmrDTO getLastEmrByPatientMrn(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        Emr emr = patient.getEmr().getLast();
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    public EmrDTO updateEmr(String mrn, EmrUpdateDTO emrUpdateDto) {
        Patient patient = findPatientOrThrow(mrn);
        Emr emr = patient.getEmr().getLast();
        if (emrUpdateDto.getHeight() != null) emr.setHeight(emrUpdateDto.getHeight());
        if (emrUpdateDto.getWeight() != null) emr.setWeight(emrUpdateDto.getWeight());
        if (emrUpdateDto.getGfr() != null) emr.setGfr(emrUpdateDto.getGfr());
        if (emrUpdateDto.getSat() != null) emr.setSat(emrUpdateDto.getSat());
        if (emrUpdateDto.getPlt() != null) emr.setPlt(emrUpdateDto.getPlt());
        if (emrUpdateDto.getWbc() != null) emr.setWbc(emrUpdateDto.getWbc());
        if (emrUpdateDto.getChildPughScore() != null) emr.setChildPughScore(emrUpdateDto.getChildPughScore());
        if (emrUpdateDto.getSodium() != null) emr.setSodium(emrUpdateDto.getSodium());

        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmrDTO> getAllEmrByPatientMrn(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        List<Emr> emrs = patient.getEmr();
        return emrs.stream().map(emr -> modelMapper.map(emr, EmrDTO.class)).collect(Collectors.toList());
    }

    //Recommendation methods

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationWithVasDTO> getAllPendingRecommendations() {
        // 1. Достаём из базы все рекомендации, у которых статус = PENDING
        List<Recommendation> recommendations = recommendationRepository.findByStatus(RecommendationStatus.PENDING);

        // 2. Пробегаемся по каждой найденной рекомендации и формируем комбинированный DTO
        return recommendations.stream().map(recommendation -> {
            // 2.1. Получаем MRN пациента, которому принадлежит эта рекомендация
            String mrn = recommendation.getPatient().getMrn();

            // 2.2. Маппим саму Recommendation в RecommendationWithVasDTO (DTO-обёртку)
            RecommendationWithVasDTO recommendationWithVasDTO =
                    modelMapper.map(recommendation, RecommendationWithVasDTO.class);

            // 2.3. Внутри RecommendationDTO есть опциональное поле patientMrn,
            // которое мы вручную задаём — оно нужно фронту для идентификации пациента
            recommendationWithVasDTO.getRecommendation().setPatientMrn(mrn);

            // 2.4. Берём у пациента последнюю VAS-жалобу (getLast()) и тоже маппим в VasDTO
            VasDTO vasDTO = modelMapper.map(recommendation.getPatient().getVas().getLast(), VasDTO.class);

            // 2.5. Подшиваем VasDTO внутрь нашего комбинированного RecommendationWithVasDTO
            recommendationWithVasDTO.setVas(vasDTO);
            // 2.6. Возвращаем готовый объект
            return recommendationWithVasDTO;
        }).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationWithVasDTO getLastRecommendationByMrn(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        Recommendation recommendation = patient.getRecommendations().getLast();
        Vas vas = patient.getVas().getLast();
        RecommendationWithVasDTO dto = new RecommendationWithVasDTO();
        dto.setRecommendation(modelMapper.map(recommendation, RecommendationDTO.class));
        dto.setVas(modelMapper.map(vas, VasDTO.class));
        dto.getRecommendation().setPatientMrn(patient.getMrn());
        return dto;               //===============> Если status == PENDING, фронт рисует кнопки Approve/Reject.
                                  //Если status == APPROVED или REJECTED, кнопок нет — чисто просмотр.


    }


    @Override
    public RecommendationDTO approveRecommendation(String mrn, RecommendationApprovalRejectionDTO dto) {
        Patient patient = findPatientOrThrow(mrn);
        Recommendation recommendation = patient.getRecommendations().getLast();
        // Проверяем, что рекомендация ещё в статусе PENDING
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation is not in PENDING status");
        }
        recommendation.setStatus(RecommendationStatus.APPROVED);
        // Если есть комментарий от доктора — добавляем в список
        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            recommendation.getComments().add(dto.getComment());
        }
        recommendationRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    public RecommendationDTO rejectRecommendation(String mrn, RecommendationApprovalRejectionDTO dto) {
        Patient patient = findPatientOrThrow(mrn);
        Recommendation recommendation = patient.getRecommendations().getLast();
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation is not in PENDING status");
        }
        recommendation.setStatus(RecommendationStatus.REJECTED);
        if (dto.getRejectedReason() == null || dto.getRejectedReason().isBlank()) {
            throw new IllegalArgumentException("Rejected reason must be provided");
        }
        recommendation.setRejectedReason(dto.getRejectedReason());
        // Дополнительно: если есть комментарий — добавляем
        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            recommendation.getComments().add(dto.getComment());
        }
        recommendationRepository.save(recommendation);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }


// TODO Данный функционал по Аудиту будет реализован в отдельном классе Spring AOP для перехвата всех действий
//1)
// @Aspect
//@Component
// public class AuditTrailAspect
//        AuditTrail audit = new AuditTrail();
//        audit.setAction(PatientRegistrationAuditAction.PATIENT_REGISTERED);//enum
//        audit.setPerson(createdBy);
//        audit.setPid(patients.getId());
//        auditTrailRepository.save(audit);

//2)
//  Создать Enum для всех действий для AuditTrailRepository
//PatientAuditAction → создание/удаление/обновление пациента.
// EmrAuditAction → изменения в медкартах.
// VasAuditAction → жалобы.
//RecommendationAuditAction → approve/reject/modify.
//AuthAuditAction → логин/логаут.
//3)
//Пометить все методы для адита @Auditable(action = PatientRegistrationAuditAction.RECOMMENDATION_REJECTED)

        }
