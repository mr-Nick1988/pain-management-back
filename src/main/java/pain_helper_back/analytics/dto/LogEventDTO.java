package pain_helper_back.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEventDTO {
    private String id;
    private LocalDateTime timestamp;
    private String className;
    private String methodName;
    private String methodSignature;
    private String arguments;
    private Long durationMs;
    private Boolean success;
    private String errorMessage;
    private String errorStackTrace;
    private String userId;
    private String sessionId;
    private String logCategory;
    private String level;
    private String module;
    private String traceId;
    private String spanId;
}
