package pain_helper_back.pain_escalation_tracking.scheduler;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Vas;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;
import pain_helper_back.pain_escalation_tracking.service.PainEscalationService;

import java.time.LocalDateTime;
import java.util.List;

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

    /*
     * Автоматическая проверка пациентов с высоким уровнем боли
     * Выполняется каждые 15 минут
     */
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
        log.info("Generating daily escalation summary...");

        try {
            List<pain_helper_back.anesthesiologist.entity.Escalation> recentEscalations = 
                    painEscalationService.findRecentEscalations(100);

            // Фильтруем эскалации за последние 24 часа
            LocalDateTime yesterday = LocalDateTime.now().minusHours(24);
            long last24hCount = recentEscalations.stream()
                    .filter(e -> e.getCreatedAt().isAfter(yesterday))
                    .count();

            long criticalCount = recentEscalations.stream()
                    .filter(e -> e.getCreatedAt().isAfter(yesterday))
                    .filter(e -> e.getPriority() == EscalationPriority.CRITICAL)
                    .count();

            long pendingCount = recentEscalations.stream()
                    .filter(e -> e.getStatus() == EscalationStatus.PENDING)
                    .count();

            log.info("=== DAILY ESCALATION SUMMARY ===");
            log.info("Escalations in last 24h: {}", last24hCount);
            log.info("Critical escalations: {}", criticalCount);
            log.info("Currently pending: {}", pendingCount);
            log.info("================================");

        } catch (Exception e) {
            log.error("Error generating daily summary: {}", e.getMessage(), e);
        }
    }
}
