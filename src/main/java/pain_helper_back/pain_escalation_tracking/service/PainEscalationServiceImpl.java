package pain_helper_back.pain_escalation_tracking.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pain_helper_back.analytics.event.VasRecordedEvent;
import pain_helper_back.common.patients.dto.exceptions.NotFoundException;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.pain_escalation_tracking.config.PainEscalationConfig;
import pain_helper_back.pain_escalation_tracking.dto.PainEscalationCheckResultDTO;
import pain_helper_back.pain_escalation_tracking.dto.PainTrendAnalysisDTO;
import pain_helper_back.pain_escalation_tracking.entity.PainEscalation;
import pain_helper_back.pain_escalation_tracking.repository.PainEscalationRepository;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Реализация модуля анализа роста боли (Pain Escalation Tracking).
 * <p>
 * Основные функции:
 * - анализирует историю VAS и вычисляет тренд боли;
 * - фиксирует рост боли ≥ 2 пунктов;
 * - создаёт PainEscalation запись и уведомляет анестезиолога.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PainEscalationServiceImpl implements PainEscalationService {

    private final PatientRepository patientRepository;
    private final PainEscalationRepository painEscalationRepository;
    private final PainEscalationConfig config;
    private final PainEscalationNotificationService notificationService;

    // Слушает событие в методах создания новых VAS
    @EventListener
    @Transactional(readOnly = true)
    public void onVasRecorded(VasRecordedEvent event) {
        String mrn = event.getPatientMrn();
        Integer vasLevel = event.getVasLevel();
        try {
            log.info("Received VAS event for patient {} (painLevel={})", mrn, vasLevel);
            handleNewVasRecord(mrn, vasLevel);
        } catch (Exception e) {
            log.error("Failed to handle VAS escalation for {}: {}", mrn, e.getMessage(), e);
        }
    }


    @Transactional(readOnly = true)
    public Patient getPatientByMrn(String mrn) {
        return patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new NotFoundException("Patient not found: " + mrn));
    }

    // ------------------------------------------------------------
    // Анализ тренда боли (для UI / аналитики) - динамику боли за всё время наблюдения.
    // 1 жалоба (VAS) → 1 рекомендация → действует в течение ~суток → следующая жалоба создаёт новую рекомендацию.
    // ------------------------------------------------------------
    @Override
    @Transactional(readOnly = true)
    public PainTrendAnalysisDTO analyzePainTrend(String mrn) {
        Patient patient = getPatientByMrn(mrn);
        List<Vas> vasHistory = patient.getVas();

        // Если нет данных или только одна жалоба — возвращаем «пустую аналитику»
        if (vasHistory == null || vasHistory.size() < 2) {
            return PainTrendAnalysisDTO.builder()
                    .patientMrn(mrn)
                    .painTrend("NO_DATA")
                    .vasRecordCount(vasHistory == null ? 0 : vasHistory.size())
                    .averageVas((double) 0)
                    .maxVas(0)
                    .minVas(0)
                    .currentVas(0)
                    .previousVas(0)
                    .vasChange(0)
                    .daysBetweenVasRecords(0)
                    .vasHistory(List.of())
                    .build();
        }

        // гарантируем правильный порядок
      

        List<Integer> vasValues = vasHistory.stream().map(Vas::getPainLevel).toList();
        int current = vasValues.getLast();
        int previous = vasValues.get(vasValues.size() - 2);
        int change = current - previous;

        String trend = "STABLE";
        if (change >= 1) trend = "INCREASING";
        else if (change <= -1) trend = "DECREASING";

        double avg = vasValues.stream().mapToInt(Integer::intValue).average().orElse(0);
        int max = vasValues.stream().mapToInt(Integer::intValue).max().orElse(0);
        int min = vasValues.stream().mapToInt(Integer::intValue).min().orElse(0);

        int daysBetween = Math.toIntExact(Duration.between(
                vasHistory.get(vasHistory.size() - 2).getRecordedAt(),
                vasHistory.getLast().getRecordedAt()
        ).toDays());

        return PainTrendAnalysisDTO.builder()
                .patientMrn(mrn)
                .painTrend(trend)
                .vasRecordCount(vasHistory.size())
                .averageVas(avg)
                .maxVas(max)
                .minVas(min)
                .currentVas(current)
                .previousVas(previous)
                .vasChange(change)
                .vasHistory(vasValues)
                .daysBetweenVasRecords(daysBetween) // можно переименовать в daysBetweenVas
                .build();
    }

    // ------------------------------------------------------------
    // Обработка новой жалобы (новое значение VAS)
    // ------------------------------------------------------------
    @Override
    @Transactional
    public void handleNewVasRecord(String mrn, Integer newVasLevel) {
        log.info("Handling new VAS record for patient {}: VAS={}", mrn, newVasLevel);

        PainEscalationCheckResultDTO checkResult = checkPainEscalation(mrn);
        if (!checkResult.isEscalationRequired()) {
            log.info("No escalation required for patient {}", mrn);
            return;
        }

        Patient patient = getPatientByMrn(mrn);
        Recommendation lastRecommendation = patient.getRecommendations().getLast();

        // создаём запись PainEscalation
        PainEscalation escalation = new PainEscalation();
        escalation.setPatient(patient);
        escalation.setLastRecommendation(lastRecommendation);
        escalation.setCreatedAt(LocalDateTime.now());
        escalation.setPreviousVas(checkResult.getPreviousVas());
        escalation.setCurrentVas(checkResult.getCurrentVas());
        escalation.setVasChange(checkResult.getVasChange());
        escalation.setPriority(checkResult.getEscalationPriority());


        painEscalationRepository.save(escalation);

        // уведомляем анестезиолога
        notificationService.sendEscalationNotification(escalation);

        log.info("Pain escalation recorded and notification sent for patient {}", mrn);
    }

    // ------------------------------------------------------------
    //  Приватная логика проверки роста боли через класс PainEscalationCheckResultDTO -
    // это результат аналитической проверки, которую делает метод checkPainEscalation().
    // DTO удобно переносит всю совокупность этих данных одним объектом, без кучи отдельных параметров.
    // ------------------------------------------------------------
    @Transactional(readOnly = true)
    protected PainEscalationCheckResultDTO checkPainEscalation(String mrn) {
        Patient patient = getPatientByMrn(mrn);

        List<Vas> vasHistory = patient.getVas();
        if (vasHistory == null || vasHistory.size() < 2) {
            return buildNoEscalationResult(mrn); // возвращаем пустой результат
        }

        List<Vas> sorted = vasHistory.stream()
                .sorted(Comparator.comparing(Vas::getRecordedAt).reversed())
                .toList();

        Vas currentVas = sorted.get(0);
        Vas previousVas = sorted.get(1);

        int diff = currentVas.getPainLevel() - previousVas.getPainLevel();
        boolean escalationRequired = diff >= config.getMinVasIncrease();  // показатель разницы от 2 и более - эскалация требуется

        EscalationPriority priority =
                currentVas.getPainLevel() >= config.getCriticalVasLevel() ? EscalationPriority.CRITICAL :
                        currentVas.getPainLevel() >= config.getHighVasLevel() ? EscalationPriority.HIGH :
                                escalationRequired ? EscalationPriority.MEDIUM : EscalationPriority.LOW;   // определяем критичность новой жалобы

        String reason = escalationRequired
                ? String.format("Pain increased by %d points (%d → %d)", diff,
                previousVas.getPainLevel(), currentVas.getPainLevel())
                : "No significant pain increase";

        String recommendations = escalationRequired
                ? "Notify anesthesiologist. Review treatment if escalation persists."
                : "Continue standard observation.";

        PainTrendAnalysisDTO trend = analyzePainTrend(mrn);

        return PainEscalationCheckResultDTO.builder()
                .patientMrn(mrn)
                .escalationRequired(escalationRequired)
                .escalationReason(reason)
                .escalationPriority(priority)
                .currentVas(currentVas.getPainLevel())
                .previousVas(previousVas.getPainLevel())
                .vasChange(diff)
                .recommendations(recommendations)
                .painTrendAnalysisDTO(trend)
                .build();
    }

    // ------------------------------------------------------------
    //  Если нет данных — возвращаем “пустой” результат
    // ------------------------------------------------------------
    private PainEscalationCheckResultDTO buildNoEscalationResult(String mrn) {
        return PainEscalationCheckResultDTO.builder()
                .patientMrn(mrn)
                .escalationRequired(false)
                .escalationReason("Insufficient VAS history")
                .escalationPriority(EscalationPriority.LOW)
                .recommendations("Monitor patient regularly.")
                .build();
    }
}