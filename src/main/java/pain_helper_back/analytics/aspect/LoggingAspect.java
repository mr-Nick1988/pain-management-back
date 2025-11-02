package pain_helper_back.analytics.aspect;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pain_helper_back.analytics.dto.LogEventDTO;

import java.time.LocalDateTime;
import java.util.UUID;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${logging.kafka.enabled:true}")
    private boolean kafkaEnabled;

    private static final String LOGGING_TOPIC = "logging-events";

    @Around("execution(* pain_helper_back..service..*.*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String methodSignature = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();
        Exception exception = null;

        try {
            return joinPoint.proceed();
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            sendToKafka(className, methodName, methodSignature, args, durationMs, exception);
        }
    }

    private void sendToKafka(String className, String methodName, String methodSignature,
                             Object[] args, long durationMs, Exception exception) {
        if (!kafkaEnabled) {
            return;
        }

        try {
            String module = determineModule(className);
            String logCategory = exception != null ? "ERROR" : (durationMs > 1000 ? "WARN" : "INFO");

            LogEventDTO logEvent = LogEventDTO.builder()
                    .id(UUID.randomUUID().toString())
                    .timestamp(LocalDateTime.now())
                    .className(className)
                    .methodName(methodName)
                    .methodSignature(methodSignature)
                    .arguments(formatArguments(args))
                    .durationMs(durationMs)
                    .success(exception == null)
                    .errorMessage(exception != null ? exception.getMessage() : null)
                    .errorStackTrace(exception != null ? getStackTrace(exception) : null)
                    .logCategory(logCategory)
                    .level(logCategory)
                    .module(module)
                    .build();

            kafkaTemplate.send(LOGGING_TOPIC, logEvent.getId(), logEvent);

        } catch (Exception e) {
            log.error("Failed to send log to Kafka: {}", e.getMessage());
        }
    }

    private String determineModule(String className) {
        if (className.contains("Nurse")) return "nurse";
        if (className.contains("Doctor")) return "doctor";
        if (className.contains("Anesthesiologist")) return "anesthesiologist";
        if (className.contains("Emr") || className.contains("Fhir")) return "external_emr_integration_service";
        if (className.contains("Admin")) return "admin";
        if (className.contains("Treatment")) return "treatment_protocol";
        return "unknown";
    }

    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) return "[]";
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            Object arg = args[i];
            switch (arg) {
                case null -> sb.append("null");
                case String str ->
                        sb.append("\"").append(str.length() > 100 ? str.substring(0, 100) + "..." : str).append("\"");
                case Number number -> sb.append(arg);
                default -> sb.append(arg.getClass().getSimpleName());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    private String getStackTrace(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getName()).append(": ").append(exception.getMessage()).append("\n");
        StackTraceElement[] stackTrace = exception.getStackTrace();
        int limit = Math.min(5, stackTrace.length);
        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(stackTrace[i].toString()).append("\n");
        }
        if (stackTrace.length > limit) {
            sb.append("\t... ").append(stackTrace.length - limit).append(" more");
        }
        return sb.toString();
    }
}