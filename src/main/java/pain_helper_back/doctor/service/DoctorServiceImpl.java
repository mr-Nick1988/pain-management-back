package pain_helper_back.doctor.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.exceptions.EntityExistsException;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.doctor.dto.RecommendationApprovalRejectionDTO;
import pain_helper_back.doctor.dto.RecommendationWithVasDTO;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;
import pain_helper_back.enums.PatientsGenders;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class DoctorServiceImpl implements DoctorService {
    private final RecommendationRepository recommendationRepository;
    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;

    /**
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
        // Проверка уникальности телефона
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

        // Если status == PENDING, фронт рисует кнопки Approve/Reject.
        // Если status == APPROVED или REJECTED, кнопок нет — чисто просмотр.
        return dto;
    }

    // ================= WORKFLOW: APPROVAL/REJECTION ================= //

    @Override
    public RecommendationDTO approveRecommendation(String mrn, RecommendationApprovalRejectionDTO dto) {
        log.info("Approving recommendation for patient MRN: {}", mrn);

        Patient patient = findPatientOrThrow(mrn);
        Recommendation recommendation = patient.getRecommendations().getLast();

        // Проверяем, что рекомендация ещё в статусе PENDING
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation is not in PENDING status");
        }

        // 1. Обновляем статус на APPROVED_BY_DOCTOR
        recommendation.setStatus(RecommendationStatus.APPROVED_BY_DOCTOR);

        // 2. Сохраняем информацию о враче (TODO: получить из Security Context)
        recommendation.setDoctorId("doctor_id"); // TODO: заменить на реальный ID из Security Context
        recommendation.setDoctorActionAt(LocalDateTime.now());
        recommendation.setDoctorComment(dto.getComment());

        // 3. Финальное одобрение (т.к. нет эскалации)
        recommendation.setStatus(RecommendationStatus.FINAL_APPROVED);
        recommendation.setFinalApprovedBy(recommendation.getDoctorId());
        recommendation.setFinalApprovalAt(LocalDateTime.now());

        // 4. Если есть комментарий от доктора — добавляем в список
        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            recommendation.getComments().add("Doctor: " + dto.getComment());
        }

        recommendationRepository.save(recommendation);

        log.info("Recommendation approved: id={}, status={}", recommendation.getId(), recommendation.getStatus());

        return modelMapper.map(recommendation, RecommendationDTO.class);
    }

    @Override
    public RecommendationDTO rejectRecommendation(String mrn, RecommendationApprovalRejectionDTO dto) {
        log.info("Rejecting recommendation for patient MRN: {}", mrn);

        Patient patient = findPatientOrThrow(mrn);
        Recommendation recommendation = patient.getRecommendations().getLast();

        // Проверяем, что рекомендация ещё в статусе PENDING
        if (recommendation.getStatus() != RecommendationStatus.PENDING) {
            throw new IllegalStateException("Recommendation is not in PENDING status");
        }

        // Проверяем, что указана причина отказа
        if (dto.getRejectedReason() == null || dto.getRejectedReason().isBlank()) {
            throw new IllegalArgumentException("Rejected reason must be provided");
        }

        // 1. Обновляем статус на REJECTED_BY_DOCTOR
        recommendation.setStatus(RecommendationStatus.REJECTED_BY_DOCTOR);

        // 2. Сохраняем информацию о враче (TODO: получить из Security Context)
        recommendation.setDoctorId("doctor_id"); // TODO: заменить на реальный ID из Security Context
        recommendation.setDoctorActionAt(LocalDateTime.now());
        recommendation.setDoctorComment(dto.getComment());
        recommendation.setRejectedReason(dto.getRejectedReason());

        // 3. СОЗДАЕМ ЭСКАЛАЦИЮ
        Escalation escalation = new Escalation();
        escalation.setRecommendation(recommendation);
        escalation.setEscalatedBy(recommendation.getDoctorId());
        escalation.setEscalatedAt(LocalDateTime.now());
        escalation.setEscalationReason(dto.getRejectedReason());
        escalation.setDescription("Doctor rejected recommendation: " + dto.getRejectedReason());

        // 4. Определяем приоритет эскалации по уровню боли (VAS)
        Vas vas = patient.getVas().stream()
                .max((v1, v2) -> v1.getCreatedAt().compareTo(v2.getCreatedAt()))
                .orElse(null);

        if (vas != null) {
            if (vas.getPainLevel() >= 8) {
                escalation.setPriority(EscalationPriority.HIGH);
            } else if (vas.getPainLevel() >= 5) {
                escalation.setPriority(EscalationPriority.MEDIUM);
            } else {
                escalation.setPriority(EscalationPriority.LOW);
            }
        } else {
            // Если VAS не найден, ставим средний приоритет
            escalation.setPriority(EscalationPriority.MEDIUM);
        }

        escalation.setStatus(EscalationStatus.PENDING);

        // 5. Связываем эскалацию с рекомендацией
        recommendation.setEscalation(escalation);

        // 6. Меняем статус рекомендации на ESCALATED_TO_ANESTHESIOLOGIST
        recommendation.setStatus(RecommendationStatus.ESCALATED_TO_ANESTHESIOLOGIST);

        // 7. Дополнительно: если есть комментарий — добавляем
        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            recommendation.getComments().add("Doctor: " + dto.getComment());
        }

        // 8. Сохраняем рекомендацию (cascade сохранит и escalation)
        recommendationRepository.save(recommendation);

        log.info("Recommendation rejected and escalated: recommendationId={}, escalationId={}, priority={}",
                recommendation.getId(), recommendation.getEscalation().getId(), escalation.getPriority());

        return modelMapper.map(recommendation, RecommendationDTO.class);
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
}