package pain_helper_back.performance_SLA_monitoring.config;


import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация SLA порогов для различных операций системы.
 * Пороги задаются в миллисекундах через application.properties.
 */
@Configuration
@ConfigurationProperties(prefix = "performance.sla")
@Getter
public class PerformanceSlaConfig {
    /**
     * SLA пороги для различных операций (в миллисекундах)
     */
    private final Map<String, Long> thresholds = new HashMap<>();

    /*
     * Инициализация дефолтных значений SLA
     */
    public PerformanceSlaConfig() {
        // Критические операции
        thresholds.put("recommendation.generate", 2000L);      // Генерация рекомендации < 2s
        thresholds.put("recommendation.approve", 1000L);       // Одобрение < 1s
        thresholds.put("recommendation.reject", 1000L);        // Отклонение < 1s
        // Операции с данными
        thresholds.put("patient.load", 3000L);                 // Загрузка пациента < 3s
        thresholds.put("vas.create", 1000L);                   // Создание VAS < 1s
        thresholds.put("emr.create", 2000L);                   // Создание EMR < 2s
        thresholds.put("emr.sync", 5000L);                     // Синхронизация EMR < 5s
        // Эскалации
        thresholds.put("escalation.check", 1500L);             // Проверка эскалации < 1.5s
        thresholds.put("escalation.create", 1000L);            // Создание эскалации < 1s
        thresholds.put("escalation.resolve", 1000L);           // Разрешение эскалации < 1s
        // Протоколы лечения
        thresholds.put("protocol.load", 2000L);                // Загрузка протокола < 2s
        thresholds.put("protocol.apply", 1500L);               // Применение правил < 1.5s
        // Отчеты
        thresholds.put("report.generate", 5000L);              // Генерация отчета < 5s
        thresholds.put("report.export", 3000L);                // Экспорт отчета < 3s
        // Аналитика
        thresholds.put("analytics.query", 2000L);              // Запрос аналитики < 2s
        thresholds.put("kpi.calculate", 3000L);                // Расчет KPI < 3s
    }
    /*
     * Получить SLA порог для операции
     */
    public Long getThreshold(String operationName) {
        return thresholds.getOrDefault(operationName, 5000L);//// Дефолт 5 секунд

    }
    /*
     * Проверить, нарушен ли SLA
     */
    public boolean isSlaViolated(String operationName, long executionTimeMs) {
        Long threshold = getThreshold(operationName);
        return executionTimeMs > threshold;
    }
    /*
     * Получить процент от SLA порога
     */
    public double getSlaPercentage(String operationName, long executionTimeMs) {
        Long threshold = getThreshold(operationName);
        return (executionTimeMs * 100.0) / threshold;
    }
}
