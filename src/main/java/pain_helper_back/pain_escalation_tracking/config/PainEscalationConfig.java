package pain_helper_back.pain_escalation_tracking.config;


import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация пороговых значений для эскалации боли
 */
@Configuration
@ConfigurationProperties(prefix = "pain.escalation")
@Getter
public class PainEscalationConfig {
    /**
     * Минимальный рост VAS для эскалации (по умолчанию 2 балла)
     */
    private int minVasIncrease = 2;
    /**
     * Минимальный интервал между дозами в часах (по умолчанию 4 часа)
     */
    private int minDoseIntervalHours = 4;
    /**
     * Критический уровень VAS (по умолчанию 8)
     */
    private int criticalVasLevel = 8;
    /**
     * Высокий уровень VAS (по умолчанию 6)
     */
    private int highVasLevel = 6;
    /**
     * Период анализа тренда боли в часах (по умолчанию 24 часа)
     */
    private int trendAnalysisPeriodHours = 24;
    /**
     * Максимальное количество эскалаций за период (по умолчанию 3)
     */
    private int maxEscalationsPerPeriod = 3;
}
