package pain_helper_back;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/*
 * Главный класс приложения Pain Management Assistant.
 * 
 * ВКЛЮЧЕННЫЕ ФУНКЦИИ:
 * - @SpringBootApplication - автоконфигурация Spring Boot
 * - @EnableScheduling - включение запланированных задач (@Scheduled)
 * 
 * ЗАПЛАНИРОВАННЫЕ ЗАДАЧИ:
 * - EmrSyncScheduler.syncAllFhirPatients() - каждые 6 часов (00:00, 06:00, 12:00, 18:00)
 * - PainMonitoringScheduler.monitorHighPainPatients() - каждые 15 минут
 * - PainMonitoringScheduler.checkOverdueDoses() - каждый час
 * - PainMonitoringScheduler.dailyEscalationSummary() - ежедневно в 08:00
 */
@SpringBootApplication
@EnableScheduling  // Включаем поддержку @Scheduled для автоматической EMR синхронизации
public class PainHelperBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(PainHelperBackApplication.class, args);
    }

}
