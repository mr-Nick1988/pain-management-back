package pain_helper_back.anesthesiologist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.analytics.event.RecommendationApprovedEvent;
import pain_helper_back.analytics.event.RecommendationCreatedEvent;
import pain_helper_back.analytics.event.RecommendationRejectedEvent;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.*;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.enums.Roles;
import pain_helper_back.anesthesiologist.dto.AnesthesiologistRecommendationCreateDTO;
import pain_helper_back.anesthesiologist.dto.AnesthesiologistRecommendationUpdateDTO;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class AnesthesiologistServiceImpl implements AnesthesiologistServiceInterface {


    private final RecommendationRepository recommendationRepository;
    private final PatientRepository patientRepository;
    private final ModelMapper modelMapper;
    private final ApplicationEventPublisher eventPublisher;


    // Возвращает список всех рекомендаций, переданных на уровень анестезиолога (ESCALATED).
    @Override
    @Transactional(readOnly = true)
    public List<RecommendationWithVasDTO> getAllEscalations() {
        log.info("Getting all escalations");
        return recommendationRepository.findByStatus(RecommendationStatus.ESCALATED).stream()
                .map(recommendation -> {
                    String mrn = recommendation.getPatient().getMrn();
                    RecommendationWithVasDTO dto = new RecommendationWithVasDTO();
                    dto.setRecommendation(modelMapper.map(recommendation, RecommendationDTO.class));
                    Vas lastVas = recommendation.getPatient().getVas().getLast();
                    dto.setVas(modelMapper.map(lastVas, VasDTO.class));
                    dto.setPatientMrn(mrn);

                    return dto;
                })
                .toList();
    }


    // Одобряет эскалированную рекомендацию и публикует событие для аудита.
    @Override
    @Transactional
    public RecommendationDTO approveEscalation(Long recommendationId, RecommendationApprovalRejectionDTO dto) {
        log.info("Approving escalation (Recommendation ID={}):", recommendationId);

        Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

        if (rec.getStatus() != RecommendationStatus.ESCALATED) {
            throw new IllegalStateException("Recommendation is not in ESCALATED status for approval");
        }

        String anesthId = "anesthesiologist_id"; // TODO: взять из SecurityContext
        String comment = dto.getComment();
        //  Обновляем ключевые поля
        rec.setStatus(RecommendationStatus.APPROVED);
        rec.setAnesthesiologistId(anesthId);
        rec.setAnesthesiologistActionAt(LocalDateTime.now());
        rec.setAnesthesiologistComment(comment);
        rec.setFinalApprovedBy(anesthId);
        rec.setUpdatedBy(anesthId);
        rec.setFinalApprovalAt(LocalDateTime.now());
        //  Добавляем комментарий в общий список, если он есть
        if (comment != null && !comment.isBlank()) {
            rec.getComments().add("Anesthesiologist: " + comment);
        }
        recommendationRepository.save(rec);

        //  время обработки от момента создания до финального решения анестезиолога
        Long processingTimeMs = Duration.between(
                rec.getCreatedAt(),
                rec.getAnesthesiologistActionAt()
        ).toMillis();

        eventPublisher.publishEvent(new RecommendationApprovedEvent(
                this,
                rec.getId(),
                anesthId,
                Roles.ANESTHESIOLOGIST.name(),
                rec.getPatient().getMrn(),
                rec.getAnesthesiologistActionAt(),
                comment,
                processingTimeMs
        ));

        log.info("Escalation approved successfully: id={}, status={}", rec.getId(), rec.getStatus());
        return modelMapper.map(rec, RecommendationDTO.class);
    }


    // Отклоняет эскалированную рекомендацию и публикует событие для аудита.
    @Override
    @Transactional
    public RecommendationDTO rejectEscalation(Long recommendationId, RecommendationApprovalRejectionDTO dto) {
        log.info("Rejecting escalation (Recommendation ID={})", recommendationId);

        Recommendation rec = recommendationRepository.findById(recommendationId)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

        if (rec.getStatus() != RecommendationStatus.ESCALATED) {
            throw new IllegalStateException("Recommendation is not in ESCALATED status");
        }
        String anesthId = "anesthesiologist_id"; // TODO: взять из SecurityContext
        //  Обновляем состояние рекомендации
        rec.setStatus(RecommendationStatus.REJECTED);
        rec.setUpdatedBy(anesthId);
        rec.setAnesthesiologistId(anesthId);
        rec.setAnesthesiologistActionAt(LocalDateTime.now());
        rec.setAnesthesiologistComment(dto.getComment());
        //“Although SRS v0.2 defines that every rejection must include a reason,
        // the current implementation treats the anesthesiologist’s rejection as a new custom recommendation creation
        // rather than a rejection in the workflow sense; therefore, a reason is not required.”
        String rejectedReasons = rec.getRejectedReason() != null ? rec.getRejectedReason() : "";
        String uniRegReason = rejectedReasons.concat(", Rejection by anesthesiologist → new instruction created");
        rec.setRejectedReason(uniRegReason);
        rec.setReplacedAt(LocalDateTime.now());

        recommendationRepository.save(rec);

        //  Публикуем событие для аудита
        Long lifeCycleMs = Duration.between(rec.getCreatedAt(), rec.getReplacedAt()).toMillis();
        eventPublisher.publishEvent(new RecommendationRejectedEvent(
                this,
                rec.getId(),
                rec.getAnesthesiologistId(),
                Roles.ANESTHESIOLOGIST.name(),
                rec.getPatient().getMrn(),
                rec.getAnesthesiologistActionAt(),
                rec.getRejectedReason(),
                rec.getAnesthesiologistComment(),
                lifeCycleMs
        ));
        log.info("Escalation rejected successfully: id={}, lifecycle={} ms", rec.getId(), lifeCycleMs);
        return modelMapper.map(rec, RecommendationDTO.class);
    }


    /**
     * Создаёт новую рекомендацию вручную после отклонения предыдущей,
     * заменяя старую, чтобы количество рекомендаций соответствовало количеству VAS-записей.
     */
    @Override
    @Transactional
    public RecommendationDTO createRecommendationAfterRejection(AnesthesiologistRecommendationCreateDTO dto) {
        long startTime = System.currentTimeMillis();

        //  Находим старую рекомендацию (управляемая сущность)
        Recommendation oldRec = recommendationRepository.findById(dto.getPreviousRecommendationId())
                .orElseThrow(() -> new NotFoundException("Previous recommendation not found"));

        //  Проверяем статус — должна быть REJECTED
        if (oldRec.getStatus() != RecommendationStatus.REJECTED) {
            throw new IllegalStateException("Cannot overwrite a recommendation that is not REJECTED");
        }

        //  Находим пациента,emr и vas
        Patient patient = findPatientOrThrow(dto.getPatientMrn());
        Emr emr = patient.getEmr().getLast();
        Vas vas = patient.getVas().getLast();

        //  Обновляем все нужные поля прямо в управляемом объекте
        oldRec.setPatient(patient);
        oldRec.setRegimenHierarchy(dto.getRegimenHierarchy());
        oldRec.setStatus(RecommendationStatus.APPROVED);
        oldRec.setCreatedBy("anesthesiologist_id"); //TODO добавить из Context Spring Security
        oldRec.setCreatedAt(LocalDateTime.now());

        //  Заменяем препараты (удаляем старые, вставляем новые)
        oldRec.getDrugs().clear();
        List<DrugRecommendation> mappedDrugs = dto.getDrugs().stream()
                .map(d -> {
                    DrugRecommendation drug = modelMapper.map(d, DrugRecommendation.class);
                    drug.setId(null); // чтобы гибернейт создал заново
                    drug.setRecommendation(oldRec);
                    return drug;
                })
                .toList();
        oldRec.getDrugs().addAll(mappedDrugs);

        oldRec.setContraindications(dto.getContraindications());
        oldRec.setComments(dto.getComments());

        //  Обновляем таймштампы и флаги
        oldRec.setReplacedAt(LocalDateTime.now());
        oldRec.setGenerationFailed(false);

        //  Hibernate автоматически сделает UPDATE
        Recommendation saved = recommendationRepository.save(oldRec);

        log.info("Recommendation overwritten: id={}, patientMrn={}, status={}",
                saved.getId(), patient.getMrn(), saved.getStatus());

        long processingTime = System.currentTimeMillis() - startTime;

        // Извлекаем диагнозы для аналитики
        List<String> diagnosisCodes = emr.getDiagnoses() != null ?
                emr.getDiagnoses().stream().map(Diagnosis::getIcdCode).toList() : new ArrayList<>();
        // Извлекаем названия препаратов и дозировки из списка drugs
        List<String> drugNames = saved.getDrugs() != null ?
                saved.getDrugs().stream().map(DrugRecommendation::getDrugName).toList() : new ArrayList<>();
        List<String> dosages = saved.getDrugs() != null ?
                saved.getDrugs().stream().map(DrugRecommendation::getDosing).toList() : new ArrayList<>();
        String route = saved.getDrugs() != null && !saved.getDrugs().isEmpty() &&
                saved.getDrugs().getFirst().getRoute() != null ?
                saved.getDrugs().getFirst().getRoute().name() : "UNKNOWN";

        // Публикация события создания рекомендации
        eventPublisher.publishEvent(new RecommendationCreatedEvent(
                this,
                saved.getId(),
                patient.getMrn(),
                drugNames,
                dosages,
                route,
                vas.getPainLevel(),
                "anesthesiologistId", // TODO: заменить на реальный ID из Security Context
                saved.getCreatedAt(),
                processingTime,
                diagnosisCodes
        ));
        //  Возвращаем DTO
        return modelMapper.map(saved, RecommendationDTO.class);
    }
    // Обновляет данные эскалированной рекомендации после личного approve, изменяя препараты, противопоказания или комментарий.


    @Override
    @Transactional
    public RecommendationDTO updateRecommendation(Long id, AnesthesiologistRecommendationUpdateDTO dto) {
        log.info("Updating existing recommendation (id={})", id);

        Recommendation rec = recommendationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Recommendation not found"));

        if (rec.getStatus() != RecommendationStatus.ESCALATED) {
            throw new IllegalStateException("Only ESCALATED recommendations can be updated");
        }

        String anesthId = "anesthesiologist_id"; // TODO: из SecurityContext

        // Обновляем препараты
        if (dto.getDrugs() != null && !dto.getDrugs().isEmpty()) {
            List<DrugRecommendation> existingDrugs = rec.getDrugs();

            // проходим по лекарствам (длина пока - 2, но может стать больше)
            for (int i = 0; i < Math.min(existingDrugs.size(), dto.getDrugs().size()); i++) {

                DrugRecommendation current = existingDrugs.get(i);
                DrugRecommendationDTO update = dto.getDrugs().get(i);

                if (update.getDrugName() != null) current.setDrugName(update.getDrugName());
                if (update.getActiveMoiety() != null) current.setActiveMoiety(update.getActiveMoiety());
                if (update.getDosing() != null) current.setDosing(update.getDosing());
                if (update.getInterval() != null) current.setInterval(update.getInterval());
                if (update.getRoute() != null) current.setRoute(update.getRoute());
                if (update.getAgeAdjustment() != null) current.setAgeAdjustment(update.getAgeAdjustment());
                if (update.getWeightAdjustment() != null) current.setWeightAdjustment(update.getWeightAdjustment());
                if (update.getChildPugh() != null) current.setChildPugh(update.getChildPugh());
                if (update.getRole() != null) current.setRole(update.getRole());
            }
        }

        //  Обновляем противопоказания
        if (dto.getContraindications() != null) {
            rec.setContraindications(dto.getContraindications());
        }

        //  Обновляем комментарий (обязательно для UPDATE)
        if (dto.getComment() != null && !dto.getComment().isBlank()) {
            rec.setAnesthesiologistComment(dto.getComment());
            rec.getComments().add("Anesthesiologist (update): " + dto.getComment());
        } else {
            throw new IllegalArgumentException("Comment is required when updating a recommendation");
        }

        //  Служебные поля 
        rec.setAnesthesiologistId(anesthId);
        rec.setAnesthesiologistActionAt(LocalDateTime.now());

        log.info("Recommendation partially updated successfully (id={})", id);
        return modelMapper.map(rec, RecommendationDTO.class);
    }

    // ================= PATIENT & EMR ACCESS ================= //

    @Override
    @Transactional(readOnly = true)
    public PatientDTO getPatientByMrn(String mrn) {
        log.info("Fetching patient by MRN for anesthesiologist: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);
        return modelMapper.map(patient, PatientDTO.class);
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationWithVasDTO> getRejectedRecommendations() {
        log.info("Fetching all REJECTED recommendations without replacement");

        return recommendationRepository.findByStatus(RecommendationStatus.REJECTED).stream()
                .map(recommendation -> {
                    // Берём MRN
                    String mrn = recommendation.getPatient().getMrn();

                    // Создаём DTO-обёртку
                    RecommendationWithVasDTO dto = new RecommendationWithVasDTO();

                    // Мапим саму рекомендацию
                    dto.setRecommendation(modelMapper.map(recommendation, RecommendationDTO.class));

                    // Берём последнюю запись VAS для пациента
                    List<Vas> vasList = recommendation.getPatient().getVas();
                    if (vasList != null && !vasList.isEmpty()) {
                        Vas lastVas = vasList.get(vasList.size() - 1);
                        dto.setVas(modelMapper.map(lastVas, VasDTO.class));
                    }

                    // Добавляем MRN в DTO
                    dto.setPatientMrn(mrn);

                    return dto;
                })
                .toList();
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


    @Override
    @Transactional(readOnly = true)
    public EmrDTO getLastEmrByPatientMrn(String mrn) {
        log.info("Fetching last EMR by patient MRN for anesthesiologist: {}", mrn);
        Patient patient = findPatientOrThrow(mrn);
        if (patient.getEmr() == null || patient.getEmr().isEmpty()) {
            throw new NotFoundException("No EMR records found for patient with MRN: " + mrn);
        }
        var emr = patient.getEmr().getLast();
        return modelMapper.map(emr, EmrDTO.class);
    }

    // --- private helper method ---
    private Patient findPatientOrThrow(String mrn) {
        return patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with MRN " + mrn + " not found"));
    }
}


