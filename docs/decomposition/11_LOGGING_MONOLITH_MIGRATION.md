# 11 - Миграция монолита для Logging Service

**Предыдущий:** [10_LOGGING_SERVICE_IMPLEMENTATION.md](10_LOGGING_SERVICE_IMPLEMENTATION.md)  
**Следующий:** [12_LOGGING_SERVICE_DEPLOYMENT.md](12_LOGGING_SERVICE_DEPLOYMENT.md)

---

## 🎯 Цель

Обновить монолит для отправки логов в Kafka вместо прямой записи в MongoDB.

---

## 📋 Шаги миграции

### Шаг 1: Добавить Kafka зависимость

В `pom.xml` монолита добавьте:

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

---

### Шаг 2: Конфигурация Kafka Producer

Создайте файл `KafkaProducerConfig.java`:

```java
package pain_helper_back.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaProducerConfig {
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        config.put(ProducerConfig.ACKS_CONFIG, "1");
        config.put(ProducerConfig.RETRIES_CONFIG, 3);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

---

### Шаг 3: Создать DTO для событий

Создайте `LogEventDTO.java`:

```java
package pain_helper_back.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LogEventDTO {
    
    private String id;
    private LocalDateTime timestamp;
    private String serviceName;
    private String operation;
    private String userId;
    private String userName;
    private String logLevel;
    private String methodName;
    private String className;
    private Map<String, Object> parameters;
    private Object result;
    private String errorMessage;
    private String stackTrace;
    private Long executionTimeMs;
    private String traceId;
    private String spanId;
    private Map<String, String> metadata;
}
```

---

### Шаг 4: Обновить LoggingAspect

Обновите существующий `LoggingAspect.java`:

```java
package pain_helper_back.analytics.aspect;

import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import pain_helper_back.analytics.annotation.LogOperation;
import pain_helper_back.analytics.dto.LogEventDTO;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class LoggingAspect {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final Tracer tracer;
    
    @Value("${spring.application.name:monolith}")
    private String serviceName;
    
    @Value("${logging.kafka.enabled:true}")
    private boolean kafkaLoggingEnabled;
    
    private static final String LOGGING_TOPIC = "logging-events";
    
    @Around("@annotation(logOperation)")
    public Object logOperation(ProceedingJoinPoint joinPoint, LogOperation logOperation) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String methodName = signature.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        
        // Получить параметры метода
        Map<String, Object> parameters = extractParameters(joinPoint);
        
        // Создать событие
        LogEventDTO logEvent = LogEventDTO.builder()
                .id(UUID.randomUUID().toString())
                .timestamp(LocalDateTime.now())
                .serviceName(serviceName)
                .operation(logOperation.value().isEmpty() ? methodName : logOperation.value())
                .className(className)
                .methodName(methodName)
                .parameters(parameters)
                .logLevel("INFO")
                .build();
        
        // Добавить tracing информацию
        if (tracer != null && tracer.currentSpan() != null) {
            logEvent.setTraceId(tracer.currentSpan().context().traceId());
            logEvent.setSpanId(tracer.currentSpan().context().spanId());
        }
        
        try {
            // Выполнить метод
            Object result = joinPoint.proceed();
            
            // Рассчитать время выполнения
            long executionTime = System.currentTimeMillis() - startTime;
            logEvent.setExecutionTimeMs(executionTime);
            logEvent.setResult(sanitizeResult(result));
            
            // Отправить в Kafka асинхронно
            sendToKafka(logEvent);
            
            return result;
            
        } catch (Exception e) {
            // Обработка ошибки
            long executionTime = System.currentTimeMillis() - startTime;
            logEvent.setExecutionTimeMs(executionTime);
            logEvent.setLogLevel("ERROR");
            logEvent.setErrorMessage(e.getMessage());
            logEvent.setStackTrace(getStackTrace(e));
            
            // Отправить в Kafka
            sendToKafka(logEvent);
            
            throw e;
        }
    }
    
    private void sendToKafka(LogEventDTO logEvent) {
        if (!kafkaLoggingEnabled) {
            log.debug("Kafka logging disabled, skipping: {}", logEvent.getOperation());
            return;
        }
        
        try {
            kafkaTemplate.send(LOGGING_TOPIC, logEvent.getId(), logEvent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send log event to Kafka: {}", ex.getMessage());
                    } else {
                        log.trace("Log event sent to Kafka: {}", logEvent.getOperation());
                    }
                });
        } catch (Exception e) {
            log.error("Error sending log event to Kafka", e);
            // Не бросаем исключение - логирование не должно ломать бизнес-логику
        }
    }
    
    private Map<String, Object> extractParameters(ProceedingJoinPoint joinPoint) {
        Map<String, Object> params = new HashMap<>();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String[] paramNames = signature.getParameterNames();
        Object[] paramValues = joinPoint.getArgs();
        
        for (int i = 0; i < paramNames.length; i++) {
            // Не логируем чувствительные данные
            if (isSensitiveParameter(paramNames[i])) {
                params.put(paramNames[i], "***REDACTED***");
            } else {
                params.put(paramNames[i], sanitizeValue(paramValues[i]));
            }
        }
        
        return params;
    }
    
    private boolean isSensitiveParameter(String paramName) {
        String lowerName = paramName.toLowerCase();
        return lowerName.contains("password") || 
               lowerName.contains("token") || 
               lowerName.contains("secret") ||
               lowerName.contains("credential");
    }
    
    private Object sanitizeValue(Object value) {
        if (value == null) {
            return null;
        }
        
        // Ограничить размер строк
        if (value instanceof String) {
            String str = (String) value;
            return str.length() > 1000 ? str.substring(0, 1000) + "..." : str;
        }
        
        // Для коллекций - только размер
        if (value instanceof java.util.Collection) {
            return "Collection[size=" + ((java.util.Collection<?>) value).size() + "]";
        }
        
        return value.toString();
    }
    
    private Object sanitizeResult(Object result) {
        if (result == null) {
            return null;
        }
        
        // Не логируем большие объекты
        String resultStr = result.toString();
        if (resultStr.length() > 500) {
            return resultStr.substring(0, 500) + "...";
        }
        
        return result;
    }
    
    private String getStackTrace(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getName()).append(": ").append(e.getMessage()).append("\n");
        
        // Ограничить количество строк стека
        StackTraceElement[] elements = e.getStackTrace();
        int limit = Math.min(elements.length, 10);
        
        for (int i = 0; i < limit; i++) {
            sb.append("\tat ").append(elements[i].toString()).append("\n");
        }
        
        if (elements.length > limit) {
            sb.append("\t... ").append(elements.length - limit).append(" more");
        }
        
        return sb.toString();
    }
}
```

---

### Шаг 5: Обновить application.properties

Добавьте в `application.properties`:

```properties
# Kafka Configuration
spring.kafka.bootstrap-servers=localhost:9092

# Logging Configuration
logging.kafka.enabled=true
logging.kafka.topic=logging-events

# Для production
#spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
```

---

### Шаг 6: Feature Flag для безопасного переключения

Создайте `LoggingFeatureConfig.java`:

```java
package pain_helper_back.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "logging")
public class LoggingFeatureConfig {
    
    private Kafka kafka = new Kafka();
    
    @Data
    public static class Kafka {
        private boolean enabled = true;
        private String topic = "logging-events";
        private boolean fallbackToLocal = true;
    }
    
    public boolean isKafkaEnabled() {
        return kafka.enabled;
    }
}
```

Обновите `LoggingAspect` для использования feature flag:

```java
@Autowired
private LoggingFeatureConfig featureConfig;

private void sendToKafka(LogEventDTO logEvent) {
    if (!featureConfig.isKafkaEnabled()) {
        log.debug("Kafka logging disabled by feature flag");
        if (featureConfig.getKafka().isFallbackToLocal()) {
            // Fallback к старой логике (опционально)
            logLocally(logEvent);
        }
        return;
    }
    
    // ... остальной код
}
```

---

### Шаг 7: Добавить Circuit Breaker (опционально)

Добавьте зависимость:

```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
    <version>2.1.0</version>
</dependency>
```

Обновите `LoggingAspect`:

```java
@Autowired
private CircuitBreakerRegistry circuitBreakerRegistry;

private void sendToKafka(LogEventDTO logEvent) {
    CircuitBreaker circuitBreaker = circuitBreakerRegistry.circuitBreaker("logging");
    
    Try.ofSupplier(CircuitBreaker.decorateSupplier(circuitBreaker, () -> {
        kafkaTemplate.send(LOGGING_TOPIC, logEvent.getId(), logEvent);
        return null;
    })).onFailure(ex -> {
        log.error("Circuit breaker opened for logging: {}", ex.getMessage());
        if (featureConfig.getKafka().isFallbackToLocal()) {
            logLocally(logEvent);
        }
    });
}
```

Конфигурация в `application.properties`:

```properties
# Circuit Breaker Configuration
resilience4j.circuitbreaker.instances.logging.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.logging.wait-duration-in-open-state=30s
resilience4j.circuitbreaker.instances.logging.sliding-window-size=10
```

---

### Шаг 8: Удалить старый код (после успешной миграции)

После того как Logging Service работает стабильно:

1. Удалите прямую запись в MongoDB из `LoggingAspect`
2. Удалите старый `LogEntry` entity из монолита (если есть)
3. Удалите старый `LogEntryRepository` из монолита

---

## 🧪 Тестирование

### Unit Test

```java
@SpringBootTest
@EmbeddedKafka(partitions = 1, topics = {"logging-events"})
class LoggingAspectTest {
    
    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;
    
    @Autowired
    private LoggingAspect loggingAspect;
    
    @Test
    void testLogEventSentToKafka() throws Exception {
        // Arrange
        ProceedingJoinPoint joinPoint = mock(ProceedingJoinPoint.class);
        when(joinPoint.proceed()).thenReturn("result");
        
        // Act
        loggingAspect.logOperation(joinPoint, mock(LogOperation.class));
        
        // Assert
        // Verify Kafka message was sent
        Thread.sleep(1000); // Wait for async
        // Add verification logic
    }
}
```

### Integration Test

```bash
# 1. Запустить Kafka
docker-compose up -d kafka

# 2. Запустить монолит
mvn spring-boot:run

# 3. Выполнить тестовый запрос
curl -X POST http://localhost:8080/api/doctor/patients \
  -H "Content-Type: application/json" \
  -d '{"firstName":"John","lastName":"Doe"}'

# 4. Проверить Kafka
docker exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic logging-events \
  --from-beginning
```

---

## 🔄 План постепенного переключения

### Week 1: Canary (10%)

```properties
logging.kafka.enabled=true
logging.kafka.canary-percentage=10
```

### Week 2: Увеличение (50%)

```properties
logging.kafka.canary-percentage=50
```

### Week 3: Полное переключение (100%)

```properties
logging.kafka.enabled=true
logging.kafka.canary-percentage=100
```

---

## ✅ Чеклист миграции

- [ ] Kafka зависимость добавлена
- [ ] KafkaProducerConfig создан
- [ ] LogEventDTO создан
- [ ] LoggingAspect обновлен
- [ ] Feature flags настроены
- [ ] Circuit Breaker добавлен (опционально)
- [ ] Unit тесты написаны
- [ ] Integration тесты пройдены
- [ ] Canary deployment выполнен
- [ ] Мониторинг настроен
- [ ] Старый код удален

---

**Следующий шаг:** [12_LOGGING_SERVICE_DEPLOYMENT.md](12_LOGGING_SERVICE_DEPLOYMENT.md)
