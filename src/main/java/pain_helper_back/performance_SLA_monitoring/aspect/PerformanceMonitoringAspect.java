package pain_helper_back.performance_SLA_monitoring.aspect;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import pain_helper_back.performance_SLA_monitoring.config.PerformanceSlaConfig;
import pain_helper_back.performance_SLA_monitoring.entity.PerformanceMetric;
import pain_helper_back.performance_SLA_monitoring.service.PerformanceMonitoringService;

import java.time.LocalDateTime;

/**
 * AOP Aspect для автоматического мониторинга производительности.
 * Перехватывает выполнение методов и записывает метрики.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PerformanceMonitoringAspect {

    private final PerformanceMonitoringService monitoringService;
    private final PerformanceSlaConfig slaConfig;

    /*
     * Мониторинг всех публичных методов в сервисах
     */
    @Around("execution(* pain_helper_back..service..*ServiceImpl*(..))")
    public Object monitorServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, extractOperationName(joinPoint));
    }

    /*
     * Мониторинг методов контроллеров
     */
    @Around("execution(* pain_helper_back..controller..*Controller.*(..))")
    public Object monitorControllerMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorExecution(joinPoint, extractOperationName(joinPoint));

    }

    /*
     * Основной метод мониторинга
     */
    private Object monitorExecution(ProceedingJoinPoint joinPoint, String operationName) throws Throwable {
        long startTime = System.currentTimeMillis();
        LocalDateTime timestamp = LocalDateTime.now();
        String status = "SUCCESS";
        String errorMessage = null;
        Object result = null;

        try {
            result = joinPoint.proceed();
            return result;
        } catch (Throwable throwable) {
            status = "ERROR";
            errorMessage = throwable.getMessage();
            throw throwable;
        } finally {
            long executionTime = System.currentTimeMillis() - startTime;

            // Записываем метрику асинхронно
            recordMetricAsync(joinPoint, operationName, executionTime, timestamp, status, errorMessage);
        }
    }

    /*
     * Асинхронная запись метрики
     */
    private void recordMetricAsync(ProceedingJoinPoint joinPoint, String operationName,
                                   long executionTime, LocalDateTime timestamp,
                                   String status, String errorMessage) {
        try {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
            Long slaThreshold = slaConfig.getThreshold(operationName);
            boolean slaViolated = slaConfig.isSlaViolated(operationName, executionTime);
            double slaPercentage = slaConfig.getSlaPercentage(operationName, executionTime);

            // Извлекаем параметры (опционально)
            String parameters = extractParameters(joinPoint);
            String userId = extractUserId(joinPoint);
            String userRole = extractUserRole(joinPoint);
            String patientMrn = extractPatientMrn(joinPoint);

            PerformanceMetric metric = PerformanceMetric.builder()
                    .operationName(operationName)
                    .executionTimeMs(executionTime)
                    .slaThresholdMs(slaThreshold)
                    .slaViolated(slaViolated)
                    .slaPercentage(slaPercentage)
                    .methodName(methodName)
                    .userId(userId)
                    .userRole(userRole)
                    .patientMrn(patientMrn)
                    .status(status)
                    .errorMessage(errorMessage)
                    .timestamp(timestamp)
                    .metadata(parameters)
                    .build();
            monitoringService.recordMetric(metric);
        } catch (Exception e) {
            log.error("Failed to record performance metric: {}", e.getMessage());
        }
    }

    /*
     * Извлечь название операции из метода
     */
    private String extractOperationName(ProceedingJoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        // Маппинг методов на операции
        if (methodName.contains("generateRecommendation")) {
            return "recommendation.generate";
        } else if (methodName.contains("approveRecommendation")) {
            return "recommendation.approve";
        } else if (methodName.contains("rejectRecommendation")) {
            return "recommendation.reject";
        } else if (methodName.contains("createVAS") || methodName.contains("recordVas")) {
            return "vas.create";
        } else if (methodName.contains("createEmr")) {
            return "emr.create";
        } else if (methodName.contains("syncEmr") || methodName.contains("synchronize")) {
            return "emr.sync";
        } else if (methodName.contains("checkEscalation") || methodName.contains("checkPainEscalation")) {
            return "escalation.check";
        } else if (methodName.contains("createEscalation")) {
            return "escalation.create";
        } else if (methodName.contains("resolveEscalation")) {
            return "escalation.resolve";
        } else if (methodName.contains("loadProtocol") || methodName.contains("getProtocol")) {
            return "protocol.load";
        } else if (methodName.contains("applyProtocol") || methodName.contains("applyRules")) {
            return "protocol.apply";
        } else if (methodName.contains("generateReport")) {
            return "report.generate";
        } else if (methodName.contains("exportReport") || methodName.contains("export")) {
            return "report.export";
        } else if (methodName.contains("calculateKpi") || methodName.contains("getKpi")) {
            return "kpi.calculate";
        } else if (methodName.contains("getPatient") || methodName.contains("findPatient")) {
            return "patient.load";
        } else if (methodName.contains("analytics") || methodName.contains("query")) {
            return "analytics.query";
        }
        // Дефолтное имя операции
        return className.toLowerCase().replace("serviceimpl", "") + "." + methodName;
    }

    /*
     * Извлечь параметры метода (первые 3)
     */
    private String extractParameters(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args == null || args.length == 0) {
            return null;
        }
        StringBuilder params = new StringBuilder();
        int limit = Math.min(args.length, 3);

        for (int i = 0; i < limit; i++) {
            if (args[i] != null) {
                params.append(args[i].getClass().getSimpleName())
                        .append("=")
                        .append(args[i].toString().substring(0, Math.min(50, args[i].toString().length())));
                if (i < limit - 1) {
                    params.append(", ");
                }
            }
        }
        return params.toString();
    }

    /*
     * Извлечь userId из параметров (если есть)
     */
    private String extractUserId(ProceedingJoinPoint joinPoint) {
        Object[] args = joinPoint.getArgs();
        if (args != null) {
            for (Object arg : args) {
                if (arg instanceof String && ((String) arg).startsWith("userId=")) {
                    return (String) arg;
                }
            }
        }
        return "system";// Дефолт до Spring Security
    }
    /*
     * Извлечь userRole из параметров (если есть)
     */
    private String extractUserRole(ProceedingJoinPoint joinPoint){
        String className = joinPoint.getTarget().getClass().getSimpleName();
        if (className.contains("Nurse")) {
            return "NURSE";
        } else if (className.contains("Doctor")) {
            return "DOCTOR";
        } else if (className.contains("Anesthesiologist")) {
            return "ANESTHESIOLOGIST";
        } else if (className.contains("Admin")) {
            return "ADMIN";
        }

        return "UNKNOWN";
    }
    /*
     *Извлечь patientMrn из параметров (если есть)
     */
    private String extractPatientMrn(ProceedingJoinPoint joinPoint){
        Object[] args = joinPoint.getArgs();
        if(args!= null){
            for(Object arg :args){
                if(arg instanceof String){
                    String str = (String) arg;
                    if(str.startsWith("EMR-")|| str.matches("\\d{6,}")){
                        return str;
                    }
                }
            }
        }
        return null;
    }
}
