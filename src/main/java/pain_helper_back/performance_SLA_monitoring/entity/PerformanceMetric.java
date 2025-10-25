package pain_helper_back.performance_SLA_monitoring.entity;


import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Метрика производительности операции.
 * Хранится в MongoDB для долгосрочного анализа.
 */
@Document(collection = "performance_metrics")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetric {

    @Id
    private String id;

    /**
     * Название операции (например, "recommendation.generate")
     */
    @Indexed
    private String operationName;
    /*
     * Время выполнения в миллисекундах
     */
    private Long executionTimeMs;
    /*
     * SLA порог для этой операции
     */
    private Long slaThresholdMs;
    /*
     * Нарушен ли SLA
     */
    @Indexed
    private Boolean slaViolated;
    /**
     * Процент от SLA (100% = точно на пороге, >100% = нарушение)
     */
    private Double slaPercentage;
    /*
     * Метод/класс, где выполнялась операция
     */
    private String methodName;
    /*
     * Параметры операции (для отладки)
     */
    private String userId;
    /*
     * Роль пользователя
     */
    private String userRole;
    /**
     * ID пациента (если применимо)
     */
    private String patientMrn;
    /**
     * Статус выполнения (SUCCESS/ERROR)
     */
    @Indexed
    private String status;
    /**
     * Сообщение об ошибке (если была)
     */
    private String errorMessage;
    /**
     * Время начала операции
     */
    @Indexed
    private LocalDateTime timestamp;
    /**
     * Дополнительные метаданные
     */
    private String metadata;

}
