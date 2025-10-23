package pain_helper_back.pain_escalation_tracking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.analytics.event.DoseAdministeredEvent;
import pain_helper_back.analytics.event.EscalationCreatedEvent;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.anesthesiologist.repository.TreatmentEscalationRepository;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;
import pain_helper_back.pain_escalation_tracking.config.PainEscalationConfig;
import pain_helper_back.pain_escalation_tracking.controller.PainEscalationController;
import pain_helper_back.pain_escalation_tracking.dto.*;
import pain_helper_back.pain_escalation_tracking.entity.DoseAdministration;
import pain_helper_back.pain_escalation_tracking.repository.DoseAdministrationRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/*
 * Реализация сервиса автоматической эскалации боли
 * Отслеживает рост боли и создает эскалации при критических ситуациях
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PainEscalationServiceImpl implements PainEscalationService {

    private final PatientRepository patientRepository;
    private final DoseAdministrationRepository doseAdministrationRepository;
    private final TreatmentEscalationRepository escalationRepository;
    private final PainEscalationConfig config;
    private final ApplicationEventPublisher eventPublisher;
    private final PainEscalationNotificationService notificationService;
    /*
     * Проверить необходимость эскалации боли для пациента
     * Анализирует историю VAS и дозировок для принятия решения
     */
    @Override
    @Transactional(readOnly = true)
    public PainEscalationCheckResultDTO checkPainEscalation(String mrn) {
        log.info("Checking pain escalation for patient: {}", mrn);

        Patient patient = patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with MRN " + mrn + " not found"));

        List<Vas> vasHistory = patient.getVas();
        if (vasHistory == null || vasHistory.isEmpty()) {
            return buildNoEscalationResult(mrn, "No VAS records found");
        }

        // Сортируем VAS по времени (последний первый)
        List<Vas> sortedVas = vasHistory.stream()
                .sorted(Comparator.comparing(Vas::getRecordedAt).reversed())
                .toList();

        if (sortedVas.size() < 2) {
            return buildNoEscalationResult(mrn, "Insufficient VAS history for comparison");
        }

        Vas currentVas = sortedVas.get(0);
        Vas previousVas = sortedVas.get(1);

        int vasChange = currentVas.getPainLevel() - previousVas.getPainLevel();

        // Проверяем последнюю введенную дозу
        Optional<DoseAdministration> lastDose = doseAdministrationRepository
                .findLastDoseByPatientMrn(mrn);

        boolean canAdministerDose = canAdministerNextDose(mrn);
        Long hoursSinceLastDose = null;
        LocalDateTime lastDoseTime = null;

        if (lastDose.isPresent()) {
            lastDoseTime = lastDose.get().getAdministeredAt();
            hoursSinceLastDose = Duration.between(lastDoseTime, LocalDateTime.now()).toHours();
        }

        // Анализ тренда боли за последние 24 часа
        PainTrendAnalysisDTO trendAnalysis = analyzePainTrend(mrn);

        // ЛОГИКА ПРИНЯТИЯ РЕШЕНИЯ ОБ ЭСКАЛАЦИИ
        boolean escalationRequired = false;
        String escalationReason = null;
        String escalationPriority = "LOW";
        String recommendations = "";

        // Сценарий 1: Критический уровень боли (VAS >= 8)
        if (currentVas.getPainLevel() >= config.getCriticalVasLevel()) {
            escalationRequired = true;
            escalationReason = String.format("Critical pain level: VAS %d", currentVas.getPainLevel());
            escalationPriority = "CRITICAL";
            recommendations = "URGENT: Immediate intervention required. Consider IV analgesics or anesthesiologist consultation.";
        }
        // Сценарий 2: Значительный рост боли (>= 2 балла) слишком рано после дозы
        else if (vasChange >= config.getMinVasIncrease()) {
            if (lastDose.isPresent() && hoursSinceLastDose != null) {
                if (hoursSinceLastDose < config.getMinDoseIntervalHours()) {
                    escalationRequired = true;
                    escalationReason = String.format(
                            "Pain increased by %d points only %d hours after last dose (minimum interval: %d hours)",
                            vasChange, hoursSinceLastDose, config.getMinDoseIntervalHours());
                    escalationPriority = currentVas.getPainLevel() >= config.getHighVasLevel() ? "HIGH" : "MEDIUM";
                    recommendations = "Current pain management protocol may be insufficient. Consider dose adjustment or alternative medication.";
                } else {
                    // Боль выросла, но прошло достаточно времени - можно дать следующую дозу
                    recommendations = String.format(
                            "Pain increased by %d points. Next dose can be administered (last dose was %d hours ago).",
                            vasChange, hoursSinceLastDose);
                }
            } else {
                // Нет информации о дозах, но боль выросла значительно
                escalationRequired = true;
                escalationReason = String.format("Significant pain increase: VAS %d → %d (no dose history available)",
                        previousVas.getPainLevel(), currentVas.getPainLevel());
                escalationPriority = "MEDIUM";
                recommendations = "Review pain management protocol. Consider starting analgesic therapy.";
            }
        }
        // Сценарий 3: Высокий уровень боли (VAS >= 6) с растущим трендом
        else if (currentVas.getPainLevel() >= config.getHighVasLevel() && "INCREASING".equals(trendAnalysis.getPainTrend())) {
            escalationRequired = true;
            escalationReason = String.format("High pain level (VAS %d) with increasing trend", currentVas.getPainLevel());
            escalationPriority = "MEDIUM";
            recommendations = "Monitor closely. Consider proactive pain management adjustment.";
        }

        return PainEscalationCheckResultDTO.builder()
                .patientMrn(mrn)
                .escalationRequired(escalationRequired)
                .escalationReason(escalationReason)
                .escalationPriority(escalationPriority)
                .currentVas(currentVas.getPainLevel())
                .previousVas(previousVas.getPainLevel())
                .vasChange(vasChange)
                .canAdministerNextDose(canAdministerDose)
                .lastDoseTime(lastDoseTime)
                .hoursSinceLastDose(hoursSinceLastDose)
                .requiredIntervalHours(config.getMinDoseIntervalHours())
                .recommendations(recommendations)
                .painTrendAnalysisDTO(trendAnalysis)
                .build();
    }
    /*
     * Проверить, можно ли ввести следующую дозу
     * Проверяет минимальный интервал между дозами
     */
    @Override
    @Transactional(readOnly = true)
    public boolean canAdministerNextDose(String mrn) {
        Optional<DoseAdministration> lastDose = doseAdministrationRepository
                .findLastDoseByPatientMrn(mrn);

        if (lastDose.isEmpty()) {
            return true; // Нет истории доз - можно вводить
        }

        LocalDateTime lastDoseTime = lastDose.get().getAdministeredAt();
        long hoursSinceLastDose = Duration.between(lastDoseTime, LocalDateTime.now()).toHours();

        return hoursSinceLastDose >= config.getMinDoseIntervalHours();
    }
    /**
     * Зарегистрировать введение дозы препарата
     * Сохраняет информацию для последующего анализа интервалов
     */
    @Override
    @Transactional
    public DoseAdministration registerDoseAdministration(DoseAdministration doseAdministration) {
        log.info("Registering dose administration for patient: {}",
                doseAdministration.getPatient().getMrn());

        DoseAdministration saved = doseAdministrationRepository.save(doseAdministration);

        log.info("Dose administration registered: id={}, drug={}, dosage={}",
                saved.getId(), saved.getDrugName(), saved.getDosage());

        return saved;
    }
    /*
     * Проанализировать тренд боли пациента за последние 24 часа
     * Возвращает статистику и направление изменения боли
     */
    @Override
    @Transactional(readOnly = true)
    public PainTrendAnalysisDTO analyzePainTrend(String mrn) {
        Patient patient = patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with MRN " + mrn + " not found"));

        List<Vas> vasHistory = patient.getVas();
        if (vasHistory == null || vasHistory.isEmpty()) {
            return PainTrendAnalysisDTO.builder()
                    .patientMrn(mrn)
                    .painTrend("UNKNOWN")
                    .vasRecordCount(0)
                    .build();
        }

        // Фильтруем VAS за последние 24 часа (или настроенный период)
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(config.getTrendAnalysisPeriodHours());
        List<Vas> recentVas = vasHistory.stream()
                .filter(v -> v.getRecordedAt().isAfter(cutoffTime))
                .sorted(Comparator.comparing(Vas::getRecordedAt).reversed())
                .toList();

        if (recentVas.isEmpty()) {
            return PainTrendAnalysisDTO.builder()
                    .patientMrn(mrn)
                    .painTrend("UNKNOWN")
                    .vasRecordCount(0)
                    .build();
        }

        List<Integer> vasLevels = recentVas.stream()
                .map(Vas::getPainLevel)
                .collect(Collectors.toList());

        Vas currentVas = recentVas.get(0);
        Integer currentLevel = currentVas.getPainLevel();
        Integer previousLevel = recentVas.size() > 1 ? recentVas.get(1).getPainLevel() : null;

        // Определяем тренд: INCREASING, DECREASING, STABLE
        String trend = "STABLE";
        if (previousLevel != null) {
            int change = currentLevel - previousLevel;
            if (change >= 2) {
                trend = "INCREASING";
            } else if (change <= -2) {
                trend = "DECREASING";
            }
        }

        // Вычисляем статистику
        double averageVas = vasLevels.stream()
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        int maxVas = vasLevels.stream()
                .mapToInt(Integer::intValue)
                .max()
                .orElse(0);

        int minVas = vasLevels.stream()
                .mapToInt(Integer::intValue)
                .min()
                .orElse(0);

        Long hoursSinceLastVas = previousLevel != null && recentVas.size() > 1 ?
                Duration.between(recentVas.get(1).getRecordedAt(), currentVas.getRecordedAt()).toHours() : null;

        return PainTrendAnalysisDTO.builder()
                .patientMrn(mrn)
                .currentVas(currentLevel)
                .previousVas(previousLevel)
                .vasChange(previousLevel != null ? currentLevel - previousLevel : null)
                .lastVasRecordedAt(currentVas.getRecordedAt())
                .previousVasRecordedAt(recentVas.size() > 1 ? recentVas.get(1).getRecordedAt() : null)
                .hoursSinceLastVas(hoursSinceLastVas)
                .painTrend(trend)
                .vasHistory(vasLevels)
                .averageVas(averageVas)
                .maxVas(maxVas)
                .minVas(minVas)
                .vasRecordCount(recentVas.size())
                .build();
    }
    /*
     * Автоматически обработать новую запись VAS
     * Вызывается при создании нового VAS для проверки необходимости эскалации
     */
    @Override
    @Transactional
    public void handleNewVasRecord(String mrn, Integer newVasLevel) {
        log.info("Handling new VAS record: patient={}, VAS={}", mrn, newVasLevel);

        // Проверяем необходимость эскалации
        PainEscalationCheckResultDTO checkResult = checkPainEscalation(mrn);

        if (checkResult.isEscalationRequired()) {
            log.warn("Escalation required for patient {}: {}", mrn, checkResult.getEscalationReason());

            // Создаем эскалацию
            Patient patient = patientRepository.findByMrn(mrn)
                    .orElseThrow(() -> new NotFoundException("Patient with MRN " + mrn + " not found"));

            // Получаем последнюю рекомендацию пациента для связи с эскалацией
            if (patient.getRecommendations() == null || patient.getRecommendations().isEmpty()) {
                log.warn("Cannot create escalation: patient {} has no recommendations", mrn);
                return;
            }

            Escalation escalation = new Escalation();
            escalation.setRecommendation(patient.getRecommendations().getLast());
            escalation.setEscalationReason(checkResult.getEscalationReason());
            escalation.setStatus(EscalationStatus.PENDING);
            escalation.setEscalatedBy("PAIN_ESCALATION_SERVICE");

            // Устанавливаем приоритет на основе анализа
            switch (checkResult.getEscalationPriority()) {
                case "CRITICAL" -> escalation.setPriority(EscalationPriority.CRITICAL);
                case "HIGH" -> escalation.setPriority(EscalationPriority.HIGH);
                case "MEDIUM" -> escalation.setPriority(EscalationPriority.MEDIUM);
                default -> escalation.setPriority(EscalationPriority.LOW);
            }

            Escalation savedEscalation = escalationRepository.save(escalation);

            log.info("Escalation created: id={}, priority={}, reason={}",
                    savedEscalation.getId(), savedEscalation.getPriority(), savedEscalation.getEscalationReason());

            // Публикуем событие для аналитики
            eventPublisher.publishEvent(new EscalationCreatedEvent(
                    this,
                    savedEscalation.getId(),
                    savedEscalation.getRecommendation().getId(),
                    "PAIN_ESCALATION_SERVICE",
                    mrn,
                    LocalDateTime.now(),
                    savedEscalation.getPriority(),
                    savedEscalation.getEscalationReason(),
                    newVasLevel,
                    patient.getEmr() != null && !patient.getEmr().isEmpty() && patient.getEmr().getLast().getDiagnoses() != null ?
                            patient.getEmr().getLast().getDiagnoses().stream().map(d -> d.getIcdCode()).toList() :
                            java.util.Collections.emptyList()
            ));

            // Отправляем WebSocket уведомление
            Integer previousVasLevel = checkResult.getPreviousVas();
            notificationService.sendEscalationNotification(
                    savedEscalation,
                    patient,
                    newVasLevel,
                    previousVasLevel,
                    checkResult.getRecommendations()
            );

            log.info("WebSocket notification sent to doctors about escalation for patient {}", mrn);
        } else {
            log.info("No escalation required for patient {}: {}", mrn, checkResult.getRecommendations());
        }
    }

    /*
     * Вспомогательный метод для создания результата без эскалации
     */
    private PainEscalationCheckResultDTO buildNoEscalationResult(String mrn, String reason) {
        return PainEscalationCheckResultDTO.builder()
                .patientMrn(mrn)
                .escalationRequired(false)
                .escalationReason(reason)
                .escalationPriority("NONE")
                .canAdministerNextDose(true)
                .recommendations("Continue monitoring patient pain levels.")
                .build();
    }

    /*
     * Проверить эскалацию с возможностью override VAS
     */
    @Override
    @Transactional(readOnly = true)
    public PainEscalationCheckResultDTO checkPainEscalation(String mrn, PainEscalationController.PainEscalationCheckCommand command) {
        if (command != null && command.vasLevelOverride() != null) {
            log.info("Checking pain escalation for patient {} with VAS override: {}", mrn, command.vasLevelOverride());
            // TODO: Реализовать логику с override VAS
            // Пока просто вызываем стандартную проверку
        }
        return checkPainEscalation(mrn);
    }

    /*
     * Построить DTO с информацией о доступности следующей дозы
     */
    @Override
    @Transactional(readOnly = true)
    public PainEscalationController.DoseEligibilityDTO buildDoseEligibility(String mrn) {
        log.info("Building dose eligibility for patient: {}", mrn);

        Optional<DoseAdministration> lastDose = doseAdministrationRepository.findLastDoseByPatientMrn(mrn);

        if (lastDose.isEmpty()) {
            return new PainEscalationController.DoseEligibilityDTO(
                    mrn,
                    true,
                    null,
                    config.getMinDoseIntervalHours(),
                    "No previous doses found. Can administer first dose."
            );
        }

        LocalDateTime lastDoseTime = lastDose.get().getAdministeredAt();
        long hoursSinceLastDose = Duration.between(lastDoseTime, LocalDateTime.now()).toHours();
        boolean canAdminister = hoursSinceLastDose >= config.getMinDoseIntervalHours();

        String message = canAdminister
                ? String.format("Can administer next dose. %d hours passed since last dose.", hoursSinceLastDose)
                : String.format("Cannot administer yet. Only %d hours passed. Required: %d hours.",
                hoursSinceLastDose, config.getMinDoseIntervalHours());

        return new PainEscalationController.DoseEligibilityDTO(
                mrn,
                canAdminister,
                hoursSinceLastDose,
                config.getMinDoseIntervalHours(),
                message
        );
    }

    /*
     * Зарегистрировать введение дозы через REST API
     */
    @Override
    @Transactional
    public DoseAdministrationResponseDTO registerDoseAdministration(String mrn, DoseAdministrationRequestDTO request) {
        log.info("Registering dose administration for patient: {}", mrn);

        // Проверяем существование пациента
        Patient patient = patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient with MRN " + mrn + " not found"));

        DoseAdministration doseAdministration = new DoseAdministration();
        doseAdministration.setPatient(patient);
        doseAdministration.setDrugName(request.getDrugName());
        doseAdministration.setDosage(request.getDosage());
        doseAdministration.setRoute(request.getRoute());
        doseAdministration.setAdministeredBy(request.getAdministeredBy());
        doseAdministration.setVasBefore(request.getVasBefore());
        doseAdministration.setVasAfter(request.getVasAfter());
        doseAdministration.setNotes(request.getNotes());

        // Если указана рекомендация, привязываем
        if (request.getRecommendationId() != null) {
            patient.getRecommendations().stream()
                    .filter(r -> r.getId().equals(request.getRecommendationId()))
                    .findFirst()
                    .ifPresent(doseAdministration::setRecommendation);
        }

        // Сохраняем
        DoseAdministration saved = doseAdministrationRepository.save(doseAdministration);

        log.info("Dose administration registered: id={}, drug={}, dosage={}",
                saved.getId(), saved.getDrugName(), saved.getDosage());


        // Возвращаем DTO
        return DoseAdministrationResponseDTO.builder()
                .success(true)  // Добавь, если нет
                .message("Dose registered successfully")
                .doseId(saved.getId())
                .administeredAt(saved.getAdministeredAt())
                .nextDoseAllowedAt(saved.getAdministeredAt().plusHours(config.getMinDoseIntervalHours()))
                .build();

    }

    /*
     * Получить последние эскалации
     */
    @Override
    @Transactional(readOnly = true)
    public List<Escalation> findRecentEscalations(int limit) {
        log.info("Finding {} recent escalations", limit);

        return escalationRepository.findAll().stream()
                .sorted(Comparator.comparing(Escalation::getCreatedAt).reversed())
                .limit(limit)
                .toList();
    }

    /*
     * Получить эскалацию по ID
     */
    @Override
    @Transactional(readOnly = true)
    public Escalation findEscalationById(Long id) {
        log.info("Finding escalation by id: {}", id);

        return escalationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Escalation with id " + id + " not found"));
    }

    @Override
    @Transactional
    public DoseAdministration registerDoseAdministration(
            String mrn,
            String drugName,
            Double dosage,
            String unit,
            String route,
            String administeredBy) {

        log.info("Registering dose administration for patient {}: {} {} of {}",
                mrn, dosage, unit, drugName);

        Patient patient = patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + mrn));

        DoseAdministration dose = new DoseAdministration();
        dose.setPatient(patient);
        dose.setDrugName(drugName);
        dose.setDosage(dosage);
        dose.setUnit(unit);
        dose.setRoute(route);
        dose.setAdministeredAt(LocalDateTime.now());
        dose.setAdministeredBy(administeredBy);

        // Рассчитываем время следующей допустимой дозы (минимум 4 часа)
        dose.setNextDoseAllowedAt(LocalDateTime.now().plusHours(4));

        DoseAdministration saved = doseAdministrationRepository.save(dose);

        log.info("Dose registered successfully: ID {}", saved.getId());

        // Публикуем событие для аналитики
        eventPublisher.publishEvent(new DoseAdministeredEvent(
                mrn,
                drugName,
                dosage,
                unit,
                administeredBy
        ));
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DoseHistoryDTO> getDoseHistory(String mrn) {
        log.info("Fetching dose history for patient {}", mrn);

        Patient patient = patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + mrn));

        List<DoseAdministration> doses = doseAdministrationRepository
                .findByPatientOrderByAdministeredAtDesc(patient);

        return doses.stream()
                .map(dose -> DoseHistoryDTO.builder()
                        .id(dose.getId())
                        .drugName(dose.getDrugName())
                        .dosage(dose.getDosage())
                        .unit(dose.getUnit())
                        .route(dose.getRoute())
                        .administeredAt(dose.getAdministeredAt())
                        .administeredBy(dose.getAdministeredBy())
                        .notes(dose.getNotes())
                        .nextDoseAllowedAt(dose.getNextDoseAllowedAt())
                        .build())
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Escalation getLatestEscalation(String mrn) {
        log.info("Fetching latest escalation for patient {}", mrn);

        return escalationRepository.findTopByRecommendationPatientMrnOrderByCreatedAtDesc(mrn)
                .orElse(null);
    }

    @Override
    @Transactional(readOnly = true)
    public PainEscalationStatisticsDTO getEscalationStatistics() {
        log.info("Calculating pain escalation statistics");

        List<Escalation> allEscalations = escalationRepository.findAll();

        long total = allEscalations.size();
        long pending = allEscalations.stream()
                .filter(e -> e.getStatus() == EscalationStatus.PENDING)
                .count();
        long resolved = allEscalations.stream()
                .filter(e -> e.getStatus() == EscalationStatus.RESOLVED)
                .count();
        long critical = allEscalations.stream()
                .filter(e -> e.getPriority() == EscalationPriority.CRITICAL)
                .count();
        long high = allEscalations.stream()
                .filter(e -> e.getPriority() == EscalationPriority.HIGH)
                .count();
        long medium = allEscalations.stream()
                .filter(e -> e.getPriority() == EscalationPriority.MEDIUM)
                .count();

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterday = now.minusHours(24);
        LocalDateTime weekAgo = now.minusDays(7);

        long last24h = allEscalations.stream()
                .filter(e -> e.getCreatedAt().isAfter(yesterday))
                .count();
        long last7days = allEscalations.stream()
                .filter(e -> e.getCreatedAt().isAfter(weekAgo))
                .count();

        // Средне время разрешения эскалаций
        double avgResolutionHours = allEscalations.stream()
                .filter(e -> e.getStatus() == EscalationStatus.RESOLVED && e.getResolvedAt() != null)
                .mapToLong(e -> java.time.Duration.between(e.getCreatedAt(), e.getResolvedAt()).toHours())
                .average()
                .orElse(0.0);

        return PainEscalationStatisticsDTO.builder()
                .totalEscalations(total)
                .pendingEscalations(pending)
                .resolvedEscalations(resolved)
                .criticalEscalations(critical)
                .highEscalations(high)
                .mediumEscalations(medium)
                .averageResolutionTimeHours(avgResolutionHours)
                .escalationsLast24Hours(last24h)
                .escalationsLast7Days(last7days)
                .build();
    }
}
