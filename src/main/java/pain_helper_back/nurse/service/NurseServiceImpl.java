package pain_helper_back.nurse.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.analytics.event.EmrCreatedEvent;
import pain_helper_back.analytics.event.PatientRegisteredEvent;
import pain_helper_back.analytics.event.VasRecordedEvent;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.exceptions.EntityExistsException;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.*;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.treatment_protocol.service.TreatmentProtocolService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
@Slf4j
@Service
@RequiredArgsConstructor
public class NurseServiceImpl implements NurseService {
    private final PatientRepository patientRepository;
    private final TreatmentProtocolService treatmentProtocolService;
    private final EmrRepository emrRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final RecommendationRepository recommendationRepository;





    private Patient findPatientOrThrow(String mrn) {
        return patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with this " + mrn + " not found"));
    }

    @Override
    @Transactional
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

        eventPublisher.publishEvent(new PatientRegisteredEvent(
                this,
                patient.getId(),
                mrn,
                "nurse_id", // TODO: заменить на реальный ID из Security Context
                "NURSE",
                LocalDateTime.now(),
                patient.getAge(),
                patient.getGender().toString()
        ));
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
    @Transactional(readOnly = true)
    public PatientDTO getPatientByEmail(String email) {
        Patient patient = patientRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("Patient with this email not found"));
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByPhoneNumber(String phoneNumber) {
        Patient patient = patientRepository.findByPhoneNumber(phoneNumber)
                .orElseThrow(() -> new NotFoundException("Patient with this phone number not found"));
        return modelMapper.map(patient, PatientDTO.class);
    }


    @Override
    @Transactional
    public void deletePatient(String mrn) {
        patientRepository.deleteByMrn(mrn);
    }

    @Override
    @Transactional
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
    @Transactional
    public EmrDTO createEmr(String mrn, EmrDTO emrDto) {
        // 1 Находим пациента
        Patient patient = findPatientOrThrow(mrn);
        // 2 Маппим DTO → Entity
        Emr emr = modelMapper.map(emrDto, Emr.class);
        emr.setPatient(patient);
        // 3 ВАЖНО: для Hibernate создаём "обратную связь" у каждого Diagnosis
        if (emr.getDiagnoses() != null) {
            emr.getDiagnoses().forEach(diagnosis -> diagnosis.setEmr(emr));
        }
        // 4 Добавляем EMR в коллекцию пациента
        patient.getEmr().add(emr);
        emrRepository.save(emr);

        List<String> diagnosisCodes = emr.getDiagnoses() != null ?
                emr.getDiagnoses().stream().map(Diagnosis::getIcdCode).toList() : new ArrayList<>();
        List<String> diagnosisDescriptions = emr.getDiagnoses() != null ?
                emr.getDiagnoses().stream().map(Diagnosis::getDescription).toList() : new ArrayList<>();

        eventPublisher.publishEvent(new EmrCreatedEvent(
                this,
                emr.getId(),
                mrn,
                "nurse_id", // TODO: заменить на реальный ID
                "NURSE",
                LocalDateTime.now(),
                emr.getGfr(),
                emr.getChildPughScore(),
                emr.getWeight(),
                emr.getHeight(),
                diagnosisCodes,
                diagnosisDescriptions
       ));
        // 5 Hibernate сам сохранит всё (EMR + Diagnosis) в конце транзакции
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
    @Transactional
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
        if (emrUpdateDto.getSensitivities() != null)
            emr.setSensitivities(emrUpdateDto.getSensitivities());  // new filed that we missed
        if (emrUpdateDto.getSodium() != null) emr.setSodium(emrUpdateDto.getSodium());
        if(emrUpdateDto.getDiagnoses() != null){
            emr.getDiagnoses().clear();
            // Переносим новые диагнозы из DTO → Entity
            Set<Diagnosis> updatedDiagnoses = emrUpdateDto.getDiagnoses().stream()
                    .map(diagnosisDTO -> {
                        Diagnosis d = modelMapper.map(diagnosisDTO, Diagnosis.class);
                        d.setEmr(emr); // ВАЖНО: обратная связь
                        emr.getDiagnoses().add(d);
                        return d;
                    })
                    .collect(Collectors.toSet());
        }
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional
    public VasDTO createVAS(String mrn, VasDTO vasDto) {
        Patient patient = findPatientOrThrow(mrn);
        Vas vas = modelMapper.map(vasDto, Vas.class);
        vas.setPatient(patient);
        patient.getVas().add(vas);

        // Публикация события
        eventPublisher.publishEvent(new VasRecordedEvent(
                this,
                vas.getId(),
                mrn,
                "nurse_id", // TODO: заменить на реальный ID из Security Context
                LocalDateTime.now(),
                vas.getPainLevel(),
                vas.getPainPlace(),
                vas.getPainLevel() >= 8  // isCritical если боль >= 8
        ));


        eventPublisher.publishEvent(new VasRecordedEvent(
                this,
                patient.getId(),
                mrn,
                "nurse_id", // TODO: заменить на реальный ID из Security Context
                LocalDateTime.now(),
                vasDto.getPainLevel(),
                vasDto.getPainPlace(),
                vasDto.getPainLevel() >= 8

        ));
        return modelMapper.map(vas, VasDTO.class);
    }

    @Override
    @Transactional
    public VasDTO updateVAS(String mrn, VasDTO vasDto) {
        Patient patient = findPatientOrThrow(mrn);
        Vas vas = patient.getVas().getLast();
        vas.setPainLevel(vasDto.getPainLevel());
        return modelMapper.map(vas, VasDTO.class);
    }

    @Override
    @Transactional
    public void deleteVAS(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        patient.getVas().removeLast();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VasDTO> getLastVAS(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        if (patient.getVas().isEmpty()) {
           log.warn("No VAS found for patient with MRN={}", mrn);
           return Optional.empty();
        }
        Vas vas = patient.getVas().getLast();
        VasDTO dto = modelMapper.map(vas, VasDTO.class);
        return Optional.of(dto);
    }


    @Override
    @Transactional(readOnly = true)
    public List<RecommendationDTO> getAllApprovedRecommendations() {
        // 1. Достаём из базы все рекомендации, у которых статус = PENDING
        List<Recommendation> recommendations = recommendationRepository.findByStatus(RecommendationStatus.FINAL_APPROVED);
        // 2. Пробегаемся по каждой найденной рекомендации и формируем комбинированный DTO
        return recommendations.stream().map(recommendation -> {
            // 2.1. Получаем MRN пациента, которому принадлежит эта рекомендация
            String mrn = recommendation.getPatient().getMrn();
            // 2.2. Маппим Recommendation entity в RecommendationDTO
            RecommendationDTO recommendationDTO = modelMapper.map(recommendation, RecommendationDTO.class);
            // 2.3. Внутри RecommendationDTO есть опциональное поле patientMrn,
            // которое мы вручную задаём — оно нужно фронту для идентификации пациента
            recommendationDTO.setPatientMrn(mrn);
            return recommendationDTO;
        }).toList();
    }

    @Override
    @Transactional
    public RecommendationDTO createRecommendation(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        Emr emr = patient.getEmr().getLast();
        Vas vas = patient.getVas().getLast();
        Recommendation recommendation = treatmentProtocolService.generateRecommendation( vas, patient);
        vas.setResolved(true);
        recommendation.setPatient(patient);
        patient.getRecommendations().add(recommendation);
        patientRepository.save(patient);
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional <RecommendationDTO> getLastRecommendation(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        if (patient.getRecommendations().isEmpty()) {
            log.warn("No recommendation found for patient with MRN={}", mrn);
            return Optional.empty();
        }
        Recommendation recommendation = patient.getRecommendations().getLast();
        RecommendationDTO dto = modelMapper.map(recommendation, RecommendationDTO.class);
        return Optional.of(dto);
    }
}