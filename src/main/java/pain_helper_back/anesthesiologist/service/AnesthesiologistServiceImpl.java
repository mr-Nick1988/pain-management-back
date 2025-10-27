package pain_helper_back.anesthesiologist.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.analytics.event.RecommendationApprovedEvent;
import pain_helper_back.analytics.event.RecommendationRejectedEvent;
import pain_helper_back.anesthesiologist.dto.*;
import pain_helper_back.common.patients.dto.*;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.enums.Roles;

import java.time.Duration;
import java.time.LocalDateTime;
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
                    RecommendationWithVasDTO dto = new RecommendationWithVasDTO();
                    dto.setRecommendation(modelMapper.map(recommendation, RecommendationDTO.class));

                    Vas lastVas = recommendation.getPatient().getVas().getLast();
                    dto.setVas(modelMapper.map(lastVas, VasDTO.class));
                    return dto;
                })
                .toList();
    }



    // Одобряет эскалированную рекомендацию и публикует событие для аудита.
    @Override
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
        //  Обновляем состояние рекомендации
        rec.setStatus(RecommendationStatus.REJECTED);
        rec.setAnesthesiologistId("anesthesiologist_id"); // TODO: взять из SecurityContext
        rec.setAnesthesiologistActionAt(LocalDateTime.now());
        rec.setAnesthesiologistComment(dto.getComment());
        rec.setRejectedReason(dto.getRejectedReason());
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

        // 1. Находим старую рекомендацию
        Recommendation oldRec = recommendationRepository.findById(dto.getPreviousRecommendationId())
                .orElseThrow(() -> new NotFoundException("Previous recommendation not found"));

        // 2. Находим пациента, к которому она относится
        Patient patient = patientRepository.findByMrn(dto.getPatientMrn())
                .orElseThrow(() -> new NotFoundException("Patient not found"));

        // 3. Создаём новую рекомендацию вручную (анестезиолог берёт ответственность)
        Recommendation newRec = new Recommendation();
        newRec.setId(oldRec.getId()); //  тот же ID — заменяем старую запись
        newRec.setPatient(patient);
        newRec.setRegimenHierarchy(dto.getRegimenHierarchy());
        newRec.setStatus(RecommendationStatus.APPROVED);
        newRec.setCreatedBy("anesthesiologist_id"); // TODO: из SecurityContext
        newRec.setCreatedAt(LocalDateTime.now());

        // 4. Переносим данные, введённые анестезиологом
        List<DrugRecommendation> mappedDrugs = dto.getDrugs().stream()
                .map(d -> modelMapper.map(d, DrugRecommendation.class))
                .toList();
        newRec.setDrugs(mappedDrugs);
        newRec.setContraindications(dto.getContraindications());
        newRec.setComments(dto.getComments());

        // 5. Сохраняем новую рекомендацию (перезаписывая старую)
        recommendationRepository.save(newRec);

        // 6. Обновляем коллекцию пациента — заменяем старую на новую (по индексу)
        List<Recommendation> recList = patient.getRecommendations();
        int index = recList.indexOf(oldRec);
        if (index != -1) {
            recList.set(index, newRec);
        }

        // 7. Логируем факт замены
        log.info("Recommendation replaced: id={}, patientMrn={}, oldVersionOverwritten=true",
                newRec.getId(), patient.getMrn());

        // 8. Возвращаем DTO новой рекомендации для фронта
        return modelMapper.map(newRec, RecommendationDTO.class);
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

            // проходим максимум по двум лекарствам
            for (int i = 0; i < 2; i++) {
                if (i >= existingDrugs.size() || i >= dto.getDrugs().size()) break;

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
}


