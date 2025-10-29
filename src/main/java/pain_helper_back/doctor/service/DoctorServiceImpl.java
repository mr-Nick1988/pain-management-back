package pain_helper_back.doctor.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.analytics.event.*;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.exceptions.EntityExistsException;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.*;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.common.patients.dto.RecommendationApprovalRejectionDTO;
import pain_helper_back.common.patients.dto.RecommendationWithVasDTO;
import pain_helper_back.enums.*;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DoctorServiceImpl implements DoctorService {
    private final RecommendationRepository recommendationRepository;
    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final EmrRepository emrRepository;

    /*
     * Вспомогательный метод для поиска пациента по MRN
     * @throws NotFoundException если пациент не найден
     */
    private Patient findPatientOrThrow(String mrn) {
        return patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with this " + mrn + " not found"));
    }

    // ================= PATIENTS ================= //

    @Override
    public PatientDTO createPatient(PatientDTO patientDto) {
        // Проверка уникальности email
        if (patientDto.getEmail() != null && patientRepository.existsByEmail(patientDto.getEmail())) {
            throw new EntityExistsException("Patient with this email already exists");
        }
        // Проверка уникальности телефонаК
        if (patientRepository.existsByPhoneNumber(patientDto.getPhoneNumber())) {
            throw new EntityExistsException("Patient with this phone number already exists");
        }

        // Создание пациента
        Patient patient = modelMapper.map(patientDto, Patient.class);
        patientRepository.save(patient);

        // Генерация MRN на основе ID
        String mrn = String.format("%06d", patient.getId());
        patient.setMrn(mrn);
        patientRepository.save(patient);

        eventPublisher.publishEvent(new PatientRegisteredEvent(
                this,
                patient.getId(),
                mrn,
                "doctor_id", // TODO: заменить на реальный ID из Security Context
                "DOCTOR",
                LocalDateTime.now(),
                patient.getAge(),
                patient.getGender().toString()
        ));
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
        // Создаем базовую спецификацию (пустой фильтр)
        Specification<Patient> spec = (root, query, cb) -> cb.conjunction();
        // Поиск по имени (частичное совпадение, без учета регистра)
        if (firstName != null && !firstName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("firstName")), "%" + firstName.toLowerCase().trim() + "%"));
        }
        // Поиск по фамилии (частичное совпадение, без учета регистра)
        if (lastName != null && !lastName.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("lastName")), "%" + lastName.toLowerCase().trim() + "%"));
        }
        // Поиск по статусу активности
        if (isActive != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("isActive"), isActive));
        }
        // Поиск по дате рождения (точное совпадение)
        if (birthDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("dateOfBirth"), birthDate));
        }
        // Поиск по полу (точное совпадение)
        if (gender != null && !gender.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("gender"), PatientsGenders.valueOf(gender.toUpperCase())));
        }
        // Поиск по номеру страховки (частичное совпадение)
        if (insurancePolicyNumber != null && !insurancePolicyNumber.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("insurancePolicyNumber"), "%" + insurancePolicyNumber.trim() + "%"));
        }
        // Поиск по адресу (частичное совпадение, без учета регистра)
        if (address != null && !address.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("address")), "%" + address.toLowerCase().trim() + "%"));
        }
        // Поиск по телефону (частичное совпадение)
        if (phoneNumber != null && !phoneNumber.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(root.get("phoneNumber"), "%" + phoneNumber.trim() + "%"));
        }
        // Поиск по email (частичное совпадение, без учета регистра)
        if (email != null && !email.trim().isEmpty()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("email")), "%" + email.toLowerCase().trim() + "%"));
        }
        // Выполняем поиск с комбинированными критериями
        List<Patient> patients = patientRepository.findAll(spec);
        return patients.stream()
                .map(patient -> modelMapper.map(patient, PatientDTO.class))
                .collect(Collectors.toList());
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
    public void deletePatient(String mrn) {
        patientRepository.deleteByMrn(mrn);
    }

    @Override
    public PatientDTO updatePatient(String mrn, PatientUpdateDTO patientUpdateDto) {
        Patient patient = findPatientOrThrow(mrn);

        // Обновляем только переданные (не null) поля
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

    // ================= EMR ================= //

    @Override
    public EmrDTO createEmr(String mrn, EmrDTO emrDto) {
        Patient patient = findPatientOrThrow(mrn);
        Emr emr = modelMapper.map(emrDto, Emr.class);
        emr.setPatient(patient);
        //  ВАЖНО: для Hibernate создаём "обратную связь" у каждого Diagnosis
        if (emr.getDiagnoses() != null) {
            emr.getDiagnoses().forEach(diagnosis -> diagnosis.setEmr(emr));
        }
        patient.getEmr().add(emr);
        emrRepository.save(emr);

        // Извлекаем диагнозы для аналитики
        List<String> diagnosisCodes = emr.getDiagnoses() != null ?
                emr.getDiagnoses().stream().map(Diagnosis::getIcdCode).toList() : new ArrayList<>();
        List<String> diagnosisDescriptions = emr.getDiagnoses() != null ?
                emr.getDiagnoses().stream().map(Diagnosis::getDescription).toList() : new ArrayList<>();

        // Публикация события
        eventPublisher.publishEvent(new EmrCreatedEvent(
                this,
                emr.getId(),
                mrn,
                "doctor_id",
                "DOCTOR",
                LocalDateTime.now(),
                emr.getGfr(),
                emr.getChildPughScore(),
                emr.getWeight(),
                emr.getHeight(),
                diagnosisCodes,
                diagnosisDescriptions
        ));
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

        // Обновляем только переданные (не null) поля
        if (emrUpdateDto.getHeight() != null) emr.setHeight(emrUpdateDto.getHeight());
        if (emrUpdateDto.getWeight() != null) emr.setWeight(emrUpdateDto.getWeight());
        if (emrUpdateDto.getGfr() != null) emr.setGfr(emrUpdateDto.getGfr());
        if (emrUpdateDto.getSat() != null) emr.setSat(emrUpdateDto.getSat());
        if (emrUpdateDto.getPlt() != null) emr.setPlt(emrUpdateDto.getPlt());
        if (emrUpdateDto.getWbc() != null) emr.setWbc(emrUpdateDto.getWbc());
        if (emrUpdateDto.getSensitivities() != null) emr.setSensitivities(emrUpdateDto.getSensitivities());
        if (emrUpdateDto.getChildPughScore() != null) emr.setChildPughScore(emrUpdateDto.getChildPughScore());
        if (emrUpdateDto.getSodium() != null) emr.setSodium(emrUpdateDto.getSodium());
        //Если в emrUpdateDto.getDiagnoses() ты планируешь обновлять список диагнозов (например, добавлять новые или удалять старые),
        //тогда, придётся снова пройтись и обновить связь, как в create.
        if (emrUpdateDto.getDiagnoses() != null) {
            emr.getDiagnoses().clear();
            Set<Diagnosis> updatedDiagnoses = emrUpdateDto.getDiagnoses().stream()
                    .map(diagnosisDTO -> {
                        Diagnosis d = modelMapper.map(diagnosisDTO, Diagnosis.class);
                        d.setEmr(emr);
                        emr.getDiagnoses().add(d);
                        return d;
                    })
                    .collect(Collectors.toSet());
            emr.setDiagnoses(updatedDiagnoses);
        }
        return modelMapper.map(emr, EmrDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EmrDTO> getAllEmrByPatientMrn(String mrn) {
        Patient patient = findPatientOrThrow(mrn);
        List<Emr> emrs = patient.getEmr();
        return emrs.stream().map(emr -> modelMapper.map(emr, EmrDTO.class)).collect(Collectors.toList());
    }

    // ================= RECOMMENDATIONS ================= //

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationWithVasDTO> getAllPendingRecommendations() {
        // 1. Достаём из базы все рекомендации, у которых статус = PENDING
        List<Recommendation> recommendations = recommendationRepository.findByStatus(RecommendationStatus.PENDING);

        // 2. Пробегаемся по каждой найденной рекомендации и формируем комбинированный DTO
        return recommendations.stream().map(recommendation -> {
            // 2.1. Получаем MRN пациента, которому принадлежит эта рекомендация
            String mrn = recommendation.getPatient().getMrn();

            // 2.2. Создаем RecommendationWithVasDTO вручную
            RecommendationWithVasDTO recommendationWithVasDTO = new RecommendationWithVasDTO();

            // 2.3. Маппим Recommendation entity в RecommendationDTO
            RecommendationDTO recommendationDTO = modelMapper.map(recommendation, RecommendationDTO.class);

            // 2.4. Внутри RecommendationDTO есть опциональное поле patientMrn,
            // которое мы вручную задаём — оно нужно фронту для идентификации пациента
            recommendationDTO.setPatientMrn(mrn);

            // 2.5. Устанавливаем recommendationDTO в наш комбинированный объект
            recommendationWithVasDTO.setRecommendation(recommendationDTO);

            // 2.6. Берём у пациента последнюю VAS-жалобу (getLast()) и тоже маппим в VasDTO
            VasDTO vasDTO = modelMapper.map(recommendation.getPatient().getVas().getLast(), VasDTO.class);

            // 2.7. Подшиваем VasDTO внутрь нашего комбинированного RecommendationWithVasDTO
            recommendationWithVasDTO.setVas(vasDTO);

            // 2.8. Возвращаем готовый объект
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

        // Если status == PENDING, фронт рисует кнопки Approve/Reject.
        // Если status == APPROVED или REJECTED, кнопок нет — чисто просмотр.
        return dto;
    }

    // ================= WORKFLOW: APPROVAL/REJECTION ================= //
    @Override
    public RecommendationDTO approveRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto) {
        log.info("Approving recommendation with id: {}", recommendationId);

        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

        if (recommendation.getStatus() != RecommendationStatus.PENDING &&
                recommendation.getStatus() != RecommendationStatus.ESCALATED) {
            throw new IllegalStateException("Recommendation is not in a valid status for approval");
        }

        recommendation.setStatus(RecommendationStatus.APPROVED);
        recommendation.setDoctorId("doctor_id"); // TODO: взять из SecurityContext
        recommendation.setDoctorActionAt(LocalDateTime.now());
        recommendation.setDoctorComment(dto.getComment());
        recommendation.setFinalApprovedBy(recommendation.getDoctorId());
        recommendation.setFinalApprovalAt(LocalDateTime.now());

        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            recommendation.getComments().add("Doctor: " + dto.getComment());
        }

        recommendationRepository.save(recommendation);

        Long processingTimeMs = Duration.between(
                recommendation.getCreatedAt(),
                recommendation.getDoctorActionAt()
        ).toMillis();

        eventPublisher.publishEvent(new RecommendationApprovedEvent(
                this,
                recommendation.getId(),
                recommendation.getDoctorId(),
                Roles.DOCTOR.name(),
                recommendation.getPatient().getMrn(),
                recommendation.getDoctorActionAt(),
                recommendation.getDoctorComment(),
                processingTimeMs
        ));

        log.info("Recommendation approved: id={}, status={}", recommendation.getId(), recommendation.getStatus());
        return modelMapper.map(recommendation, RecommendationDTO.class);
    }


    @Override
    public RecommendationDTO rejectRecommendation(Long recommendationId, RecommendationApprovalRejectionDTO dto) {
        log.info("Rejecting recommendation with id: {}", recommendationId);

        Recommendation recommendation = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation is not in PENDING status");
        }

        if (dto.getRejectedReason() == null || dto.getRejectedReason().isBlank()) {
            throw new IllegalArgumentException("Rejected reason must be provided");
        }

        recommendation.setStatus(RecommendationStatus.ESCALATED);
        recommendation.setDoctorId("doctor_id"); // TODO: взять из SecurityContext
        recommendation.setDoctorActionAt(LocalDateTime.now());
        recommendation.setDoctorComment(dto.getComment());
        recommendation.setRejectedReason(dto.getRejectedReason());

        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            recommendation.getComments().add("Doctor: " + dto.getComment());
        }

        recommendationRepository.save(recommendation);

        eventPublisher.publishEvent(new RecommendationRejectedEvent(
                this,
                recommendation.getId(),
                recommendation.getDoctorId(),
                Roles.DOCTOR.name(),
                recommendation.getPatient().getMrn(),
                recommendation.getDoctorActionAt(),
                recommendation.getRejectedReason(),
                recommendation.getDoctorComment(),
                null
        ));
        log.info("Recommendation rejected and escalated: recommendationId={}, status={}",
                recommendation.getId(), recommendation.getStatus());

        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationWithVasDTO> getRecommendationsWithVasByPatientMrn(String mrn) {
        Patient patient = findPatientOrThrow(mrn);

        List<Recommendation> recs = patient.getRecommendations();
        List<Vas> vasList = patient.getVas();

        int size = Math.min(recs.size(), vasList.size()); // чтобы избежать IndexOutOfBounds
        List<RecommendationWithVasDTO> result = new ArrayList<>(size);

        for (int i = 0; i < size; i++) {
            Recommendation recommendation = recs.get(i);
            Vas vas = vasList.get(i);

            RecommendationWithVasDTO dto = new RecommendationWithVasDTO();
            dto.setPatientMrn(mrn);
            dto.setRecommendation(modelMapper.map(recommendation, RecommendationDTO.class));
            dto.setVas(modelMapper.map(vas, VasDTO.class));

            result.add(dto);
        }
        // От новых к старым
        result.sort(Comparator.comparing(dto -> dto.getRecommendation().getCreatedAt(), Comparator.reverseOrder()));

        return result;
    }

}


    // TODO: Данный функционал по Аудиту будет реализован в отдельном классе Spring AOP для перехвата всех действий
    // 1) @Aspect
    //    @Component
    //    public class AuditTrailAspect
    //        AuditTrail audit = new AuditTrail();
    //        audit.setAction(PatientRegistrationAuditAction.PATIENT_REGISTERED); // enum
    //        audit.setPerson(createdBy);
    //        audit.setPid(patients.getId());
    //        auditTrailRepository.save(audit);
    //
    // 2) Создать Enum для всех действий для AuditTrailRepository
    //    PatientAuditAction → создание/удаление/обновление пациента.
    //    EmrAuditAction → изменения в медкартах.
    //    VasAuditAction → жалобы.
    //    RecommendationAuditAction → approve/reject/modify.
    //    AuthAuditAction → логин/логаут.
    //
    // 3) Пометить все методы для аудита @Auditable(action = PatientRegistrationAuditAction.RECOMMENDATION_REJECTED)
