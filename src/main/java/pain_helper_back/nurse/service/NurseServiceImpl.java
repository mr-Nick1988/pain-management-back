package pain_helper_back.nurse.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.analytics.event.EmrCreatedEvent;
import pain_helper_back.analytics.event.PatientRegisteredEvent;
import pain_helper_back.analytics.event.RecommendationCreatedEvent;
import pain_helper_back.analytics.event.VasRecordedEvent;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.exceptions.EntityExistsException;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.*;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.pain_escalation_tracking.service.PainEscalationService;
import pain_helper_back.treatment_protocol.service.TreatmentProtocolService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;


import static java.util.stream.Collectors.toList;

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
    private final PainEscalationService painEscalationService;



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
            return patients.stream().map(patient -> modelMapper.map(patient, PatientDTO.class)).collect(toList());
        }
        if (isActive != null) {
            List<Patient> patients = patientRepository.findByIsActive(isActive);
            return patients.stream().map(p -> modelMapper.map(p, PatientDTO.class)).collect(toList());
        }
        if (birthDate != null) {
            List<Patient> patients = patientRepository.findByDateOfBirth(birthDate);
            return patients.stream().map(p -> modelMapper.map(p, PatientDTO.class)).collect(toList());
        } else {
            List<Patient> patients = patientRepository.findAll();
            return patients.stream()
                    .map(patient -> modelMapper.map(patient, PatientDTO.class))
                    .collect(toList());
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
        // Извлекаем диагнозы для аналитики
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
        if (emrUpdateDto.getDiagnoses() != null) {
            // полностью чистим старые диагнозы
            emr.getDiagnoses().clear();

            // добавляем новые в ту же коллекцию (не создаём новый Set!)
            emrUpdateDto.getDiagnoses().forEach(dto -> {
                Diagnosis d = modelMapper.map(dto, Diagnosis.class);
                d.setEmr(emr); // обратная связь
                emr.getDiagnoses().add(d); // добавляем прямо в старый Set
            });
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

        // Публикация события VAS (INTERNAL источник - медсестра)
        eventPublisher.publishEvent(new VasRecordedEvent(
                this,
                vas.getId(),
                mrn,
                "nurse_id", // TODO: заменить на реальный ID из Security Context
                LocalDateTime.now(),
                vas.getPainLevel(),
                vas.getPainPlace(),
                vas.getPainLevel() >= 8,  // isCritical если боль >= 8
                "INTERNAL",  // vasSource - внутренний ввод медсестрой
                null  //deviceId - не применимо для внутреннего ввода
        ));

        //!! АВТОМАТИЧЕСКАЯ ПРОВЕРКА ЭСКАЛАЦИИ БОЛИ
        painEscalationService.handleNewVasRecord(mrn, vas.getPainLevel());

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
        List<Recommendation> recommendations = recommendationRepository.findByStatus(RecommendationStatus.APPROVED);
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
        long startTime = System.currentTimeMillis();

        Patient patient = findPatientOrThrow(mrn);
        Emr emr = patient.getEmr().getLast();
        Vas vas = patient.getVas().getLast();

        Recommendation recommendation = treatmentProtocolService.generateRecommendation(vas, patient);
        vas.setResolved(true);

        // Проверка на существование рекомендации со статусом PENDING
        if (patient.getRecommendations().getLast().getStatus() != RecommendationStatus.EXECUTED) {
            throw new EntityExistsException("Previous recommendation is still unresolved");
        }

        recommendation.setPatient(patient);
        patient.getRecommendations().add(recommendation);
        patientRepository.save(patient);

        long processingTime = System.currentTimeMillis() - startTime;

        // Извлекаем диагнозы для аналитики
        List<String> diagnosisCodes = emr.getDiagnoses() != null ?
                emr.getDiagnoses().stream().map(Diagnosis::getIcdCode).toList() : new ArrayList<>();

        // Извлекаем названия препаратов и дозировки из списка drugs
        List<String> drugNames = recommendation.getDrugs() != null ?
                recommendation.getDrugs().stream().map(DrugRecommendation::getDrugName).toList() : new ArrayList<>();
        List<String> dosages = recommendation.getDrugs() != null ?
                recommendation.getDrugs().stream().map(DrugRecommendation::getDosing).toList() : new ArrayList<>();
        String route = recommendation.getDrugs() != null && !recommendation.getDrugs().isEmpty() &&
                recommendation.getDrugs().getFirst().getRoute() != null ?
                recommendation.getDrugs().getFirst().getRoute().name() : "UNKNOWN";

        // Публикация события создания рекомендации
        eventPublisher.publishEvent(new RecommendationCreatedEvent(
                this,
                recommendation.getId(),
                patient.getMrn(),
                drugNames,
                dosages,
                route,
                vas.getPainLevel(),
                "nurse_id", // TODO: заменить на реальный ID из Security Context
                LocalDateTime.now(),
                processingTime,
                diagnosisCodes
        ));

        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    @Transactional
    public RecommendationDTO executeRecommendation(String mrn) {
        //  Находим пациента и его последнюю рекомендацию
        Patient patient = findPatientOrThrow(mrn);
        Recommendation recommendation = patient.getRecommendations().getLast();
        //  Проверяем, что её можно исполнить
        if (recommendation.getStatus() != RecommendationStatus.APPROVED) {
            throw new IllegalStateException("Only approved recommendations can be executed.");
        }
        //  Получаем список препаратов для комментария
        List<String> drugNames = recommendation.getDrugs()
                .stream()
                .map(drugRecommendation -> drugRecommendation.getDrugName() != null && drugRecommendation.getDrugName().isBlank()
                        ? drugRecommendation.getDrugName()
                        : drugRecommendation.getActiveMoiety()
                )
                .toList();
        //  Идентификатор текущей медсестры (временно — заглушка)
        String nurseId = "NurseId"; // TODO: заменить на SecurityContextHolder.getContext().getAuthentication().getName()
        //  Формируем красивое системное сообщение
        String comment = String.format("""
                        [SYSTEM]  Recommendation executed by Nurse: %s
                        Patient MRN: %s
                        Executed at: %s
                        Drugs administered: %s
                        """,
                nurseId,
                patient.getMrn(),
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")),
                String.join(", ", drugNames)
        );
        // Добавляем комментарий и обновляем статус
        recommendation.getComments().add(comment);
        recommendation.setStatus(RecommendationStatus.EXECUTED);
        recommendation.setUpdatedBy(nurseId);
        recommendation.setUpdatedAt(LocalDateTime.now());
        // Сохраняем (каскадное сохранение драг-ов произойдёт автоматически)
        recommendationRepository.save(recommendation);
        // (в будущем) публикуем Event для аналитики
        //TODO eventPublisher.publishEvent(new RecommendationExecutedEvent(...));
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }


    @Override
    @Transactional(readOnly = true)
    public Optional<RecommendationDTO> getLastRecommendation(String mrn) {
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