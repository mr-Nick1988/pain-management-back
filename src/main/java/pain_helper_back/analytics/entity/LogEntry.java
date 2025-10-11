package pain_helper_back.analytics.entity;


/*
 * MongoDB документ для хранения технических логов
 * Используется LoggingAspect для автоматического логирования всех методов сервисов
 */

import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Document(collection = "log_entries")
@Builder
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LogEntry {
    @Id
    private String id;
    @Indexed
    private LocalDateTime timestamp;
    //information about method
    private String className;
    private String methodName;
    private String methodSignature;
    //parameter of call
    private String arguments;
    //result of execution
    private Long durationMs;
    private Boolean success;
    private String errorMessage;
    private String errorStackTrace;
    //execution context
    private String userId;
    private String sessionId;
    //log category
    @Indexed
    private String logCategory;//info, warn, error
    @Indexed
    private String level; // INFO, WARN, ERROR(for API)
    @Indexed
    private String module;// nurse, doctor, anesthesiologist, external_emr_integration_service

}
