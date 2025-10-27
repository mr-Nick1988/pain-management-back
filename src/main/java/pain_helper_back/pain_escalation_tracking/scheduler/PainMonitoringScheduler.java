package pain_helper_back.pain_escalation_tracking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.common.patients.repository.RecommendationRepository;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.pain_escalation_tracking.service.PainEscalationService;

import java.time.LocalDateTime;
import java.util.List;
//TODO Нет надобности по SRS аппликации в этом классе
//В документе Pain Escalation Tracking Module сказано:
//“The system shall detect pain escalation upon receiving new
// VAS input and update Recommendation status to ESCALATED if required.”
//триггер должен быть событием записи нового VAS (через Nurse UI или автоматическое устройство);
//не по расписанию, а по факту получения данных.


/*
 * Планировщик автоматического мониторинга боли пациентов
 * Периодически проверяет пациентов с высоким уровнем боли
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PainMonitoringScheduler {

    private final PatientRepository patientRepository;
    private final PainEscalationService painEscalationService;
    private final RecommendationRepository recommendationRepository;

    /*
     * Автоматическая проверка пациентов с высоким уровнем боли
     * Выполняется каждые 15 минут
     */

    //Ответ: в теории — чтобы система “сама” обнаруживала больных, у которых боль не спадает.
    // На практике (в  проекте) — бесполезно, потому что:
    //мы уже запускаем checkPainEscalation() в handleNewVasRecord() при каждом обновлении боли;
    //у нас нет требования в SRS “автоматически переоценивать боль без новых данных”.
    // Значит — этот метод можно смело удалить. Он дублирует и нагружает систему без смысла.
    @Scheduled(fixedRate = 900000) // 15 минут = 900000 мс
    public void monitorHighPainPatients() {
        log.info("Starting automatic pain monitoring check...");

        try {
            List<Patient> allPatients = patientRepository.findAll();
            int checkedCount = 0;
            int escalationsCreated = 0;

            for (Patient patient : allPatients) {
                if (patient.getVas() == null || patient.getVas().isEmpty()) {
                    continue;
                }

                // Получаем последний VAS
                Vas lastVas = patient.getVas().stream()
                        .filter(v -> v.getRecordedAt() != null)
                        .max((v1, v2) -> v1.getRecordedAt().compareTo(v2.getRecordedAt()))
                        .orElse(null);

                if (lastVas == null) {
                    continue;
                }

                // Проверяем только пациентов с VAS >= 6 и недавними записями (последние 2 часа)
                if (lastVas.getPainLevel() >= 6 && 
                    lastVas.getRecordedAt().isAfter(LocalDateTime.now().minusHours(2))) {
                    
                    log.debug("Checking patient {} with VAS {}", patient.getMrn(), lastVas.getPainLevel());
                    
                    var checkResult = painEscalationService.checkPainEscalation(patient.getMrn());
                    checkedCount++;

                    if (checkResult.isEscalationRequired()) {
                        log.warn("Scheduled check found escalation needed for patient {}: {}", 
                                patient.getMrn(), checkResult.getEscalationReason());
                        escalationsCreated++;
                    }
                }
            }

            log.info("Pain monitoring check completed. Checked: {}, Escalations created: {}", 
                    checkedCount, escalationsCreated);

        } catch (Exception e) {
            log.error("Error during automatic pain monitoring: {}", e.getMessage(), e);
        }
    }

    /*
     * Проверка пациентов с просроченными дозами
     * Выполняется каждый час
     */

    //Он вообще не имеет данных о дозах, потому что:
    //в Patient и Vas нет истории введения доз;
    //PainEscalationService не содержит метода canAdministerNextDose().
    @Scheduled(fixedRate = 3600000) // 1 час = 3600000 мс
    public void checkOverdueDoses() {
        log.info("Starting overdue dose check...");

        try {
            List<Patient> allPatients = patientRepository.findAll();
            int overdueCount = 0;

            for (Patient patient : allPatients) {
                if (patient.getVas() == null || patient.getVas().isEmpty()) {
                    continue;
                }

                // Получаем последний VAS
                Vas lastVas = patient.getVas().stream()
                        .filter(v -> v.getRecordedAt() != null)
                        .max((v1, v2) -> v1.getRecordedAt().compareTo(v2.getRecordedAt()))
                        .orElse(null);

                if (lastVas == null) {
                    continue;
                }

                // Если VAS >= 5 и запись старше 6 часов - пациент может нуждаться в дозе
                if (lastVas.getPainLevel() >= 5 && 
                    lastVas.getRecordedAt().isBefore(LocalDateTime.now().minusHours(6))) {
                    
                    boolean canAdminister = painEscalationService.canAdministerNextDose(patient.getMrn());
                    
                    if (canAdminister) {
                        log.warn("Patient {} may need dose administration. Last VAS: {} (recorded {} hours ago)", 
                                patient.getMrn(), 
                                lastVas.getPainLevel(),
                                java.time.Duration.between(lastVas.getRecordedAt(), LocalDateTime.now()).toHours());
                        overdueCount++;
                    }
                }
            }

            log.info("Overdue dose check completed. Patients needing attention: {}", overdueCount);

        } catch (Exception e) {
            log.error("Error during overdue dose check: {}", e.getMessage(), e);
        }
    }

    /*
     * Ежедневная сводка по эскалациям
     * Выполняется каждый день в 08:00
     */

    @Scheduled(cron = "0 0 8 * * *")
    public void dailyEscalationSummary() {
        log.info("Generating daily recommendation summary...");

        try {
            LocalDateTime since = LocalDateTime.now().minusHours(24);
            List<Recommendation> recentRecommendations =
                    recommendationRepository.findAllByCreatedAtAfter(since);

            long total = recentRecommendations.size();
            long escalated = recentRecommendations.stream()
                    .filter(r -> r.getStatus() == RecommendationStatus.ESCALATED)
                    .count();
            long approved = recentRecommendations.stream()
                    .filter(r -> r.getStatus() == RecommendationStatus.APPROVED)
                    .count();
            long rejected = recentRecommendations.stream()
                    .filter(r -> r.getStatus() == RecommendationStatus.REJECTED)
                    .count();
            long pending = recentRecommendations.stream()
                    .filter(r -> r.getStatus() == RecommendationStatus.PENDING)
                    .count();

            log.info("=== DAILY RECOMMENDATION SUMMARY ===");
            log.info("Total created in last 24h: {}", total);
            log.info("Escalated: {}", escalated);
            log.info("Approved: {}", approved);
            log.info("Rejected: {}", rejected);
            log.info("Pending: {}", pending);
            log.info("====================================");

        } catch (Exception e) {
            log.error("Error generating daily recommendation summary: {}", e.getMessage(), e);
        }
    }

}
