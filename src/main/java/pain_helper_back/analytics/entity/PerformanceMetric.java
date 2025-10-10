package pain_helper_back.analytics.entity;

/*
 * MongoDB документ для хранения метрик производительности
 * Используется PerformanceAspect для мониторинга медленных операций
 */

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "performance_metrics")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PerformanceMetric {
    @Id
    private String id;
    @Indexed
    private LocalDateTime timestamp;

    //method information
    @Indexed
    private String methodName;
    private String className;
    //performance metrics
    private Long executionTimeMs;
    private Long memoryUsedBytes;
    //categorization
    private String performanceCategory;//FAST (<100ms), NORMAL (100-500ms), SLOW (500-1000ms), VERY_SLOW (>1000ms)
    @Indexed
    private String module;//nurse, doctor, anesthesiologist, external_emr_integration_service
    //alerts flag
    private Boolean requiresAttention;// true if performanceCategory is SLOW or VERY_SLOW
}
