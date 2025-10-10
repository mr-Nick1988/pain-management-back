package pain_helper_back.analytics.aspect;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import pain_helper_back.analytics.entity.LogEntry;
import pain_helper_back.analytics.repository.LogEntryRepository;

import java.time.LocalDateTime;


/*
 * AOP Aspect для автоматического логирования всех методов сервисов
 * Перехватывает вызовы методов и сохраняет информацию в MongoDB
 */
@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class LoggingAspect {
    private final LogEntryRepository logEntryRepository;

    /*
     * Перехватывает все методы в пакетах service
     * Логирует: параметры, время выполнения, результат, ошибки
     */
    @Around("execution(* pain_helper_back..service..*.*(..))")
    public Object logServiceMethod(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String methodSignature = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        long startTime = System.currentTimeMillis();
        Object result = null;
        Exception exception = null;

        try {
            //Выполняем оригинальный метод
            result = joinPoint.proceed();
            return result;
        } catch (Exception e) {
            exception = e;
            throw e;
        } finally {
            long durationMs = System.currentTimeMillis() - startTime;
            // Асинхронно сохраняем лог в MongoDB
            saveLogEntryAsync(className, methodName, methodSignature, args, durationMs, exception);
        }
    }
    /*
     * Асинхронное сохранение лога в MongoDB
     * Не блокирует основной поток выполнения
     */
    public void saveLogEntryAsync(String className, String methodName, String methodSignature, Object[] args, long durationMs, Exception exception) {

        try {
            // Определяем модуль по имени класса
            String module = determineModule(className);
            // Определяем категорию лога
            String logCategory = exception != null ? "ERROR" : (durationMs > 1000 ? "WARN" : "INFO");

            LogEntry logEntry = LogEntry.builder()
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
                    .module(module)
                    .build();
            logEntryRepository.save(logEntry);

            // Логируем в консоль для отладки
            if (exception != null) {
                log.error("Method {} failed: {}", methodSignature, exception.getMessage());
            } else if (durationMs > 1000) {
                log.warn("Slow method detected: {} took {}ms", methodSignature, durationMs);
            } else {
                log.debug("Method {} executed in {}ms", methodSignature, durationMs);
            }

        } catch (Exception e) {
            // Не бросаем исключение, чтобы не сломать основной поток
            log.error("Failed to save log entry: {}", e.getMessage());
        }
    }

    /*
     * Определяет модуль по имени класса
     */
    private String determineModule(String className) {
        if (className.contains("Nurse")) return "nurse";
        if (className.contains("Doctor")) return "doctor";
        if (className.contains("Anesthesiologist")) return "anesthesiologist";
        if (className.contains("Emr") || className.contains("Fhir")) return "external_emr_integration_service";
        if (className.contains("Admin")) return "admin";
        if (className.contains("Treatment")) return "treatment_protocol";
        return "unknown";
    }

    /*
     * Форматирует аргументы метода для логирования
     */
    private String formatArguments(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < args.length; i++) {
            if (i > 0) sb.append(", ");
            Object arg = args[i];
            switch (arg) {
                case null -> sb.append("null");
                case String str ->
                    // Ограничиваем длину строк
                        sb.append("\"").append(str.length() > 100 ? str.substring(0, 100) + "..." : str).append("\"");
                case Number number -> sb.append(arg);
                default -> sb.append(arg.getClass().getSimpleName());
            }
        }
        sb.append("]");
        return sb.toString();
    }

    /*
     * Получает stack trace из исключения
     */
    private String getStackTrace(Exception exception) {
        StringBuilder sb = new StringBuilder();
        sb.append(exception.getClass().getName()).append(": ").append(exception.getMessage()).append("\n");

        StackTraceElement[] stackTrace = exception.getStackTrace();
        int limit = Math.min(5, stackTrace.length); // Ограничиваем 5 строками
        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(stackTrace[i].toString()).append("\n");
        }

        if (stackTrace.length > limit) {
            sb.append("\t... ").append(stackTrace.length - limit).append(" more");
        }

        return sb.toString();
    }
}
