# 10 - Logging Service - Реализация

**Предыдущий:** [09_LOGGING_SERVICE_OVERVIEW.md](09_LOGGING_SERVICE_OVERVIEW.md)  
**Следующий:** [11_LOGGING_MONOLITH_MIGRATION.md](11_LOGGING_MONOLITH_MIGRATION.md)

---

## 📁 Структура проекта

```
logging-service/
├── src/
│   ├── main/
│   │   ├── java/pain/management/logging/
│   │   │   ├── LoggingServiceApplication.java
│   │   │   ├── config/
│   │   │   │   ├── KafkaConsumerConfig.java
│   │   │   │   └── MongoConfig.java
│   │   │   ├── consumer/
│   │   │   │   └── LogEventConsumer.java
│   │   │   ├── entity/
│   │   │   │   ├── LogEntry.java
│   │   │   │   └── AuditTrail.java
│   │   │   ├── repository/
│   │   │   │   ├── LogEntryRepository.java
│   │   │   │   └── AuditTrailRepository.java
│   │   │   ├── service/
│   │   │   │   ├── LoggingService.java
│   │   │   │   └── LoggingServiceImpl.java
│   │   │   ├── controller/
│   │   │   │   └── LoggingController.java
│   │   │   └── dto/
│   │   │       ├── LogEventDTO.java
│   │   │       ├── LogQueryDTO.java
│   │   │       └── LogResponseDTO.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── application-docker.yml
│   └── test/
├── pom.xml
└── Dockerfile
```

---

## 📦 pom.xml

```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0
         https://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>
    
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.5.5</version>
    </parent>
    
    <groupId>pain.management</groupId>
    <artifactId>logging-service</artifactId>
    <version>1.0.0</version>
    <name>Logging Service</name>
    
    <properties>
        <java.version>21</java.version>
        <spring-cloud.version>2023.0.0</spring-cloud.version>
    </properties>
    
    <dependencies>
        <!-- Spring Boot Web -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        
        <!-- MongoDB -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-mongodb</artifactId>
        </dependency>
        
        <!-- Kafka -->
        <dependency>
            <groupId>org.springframework.kafka</groupId>
            <artifactId>spring-kafka</artifactId>
        </dependency>
        
        <!-- Eureka Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        
        <!-- Config Client -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-config</artifactId>
        </dependency>
        
        <!-- Actuator -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        
        <!-- Micrometer Prometheus -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-registry-prometheus</artifactId>
        </dependency>
        
        <!-- Zipkin Tracing -->
        <dependency>
            <groupId>io.micrometer</groupId>
            <artifactId>micrometer-tracing-bridge-brave</artifactId>
        </dependency>
        <dependency>
            <groupId>io.zipkin.reporter2</groupId>
            <artifactId>zipkin-reporter-brave</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- Validation -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
    </dependencies>
    
    <dependencyManagement>
        <dependencies>
            <dependency>
                <groupId>org.springframework.cloud</groupId>
                <artifactId>spring-cloud-dependencies</artifactId>
                <version>${spring-cloud.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
    
    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

---

## 💻 Main Application

### LoggingServiceApplication.java

```java
package pain.management.logging;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class LoggingServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LoggingServiceApplication.class, args);
    }
}
```

---

## 📝 Entity Classes

### LogEntry.java

```java
package pain.management.logging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "log_entries")
public class LogEntry {
    
    @Id
    private String id;
    
    @Indexed
    private LocalDateTime timestamp;
    
    @Indexed
    private String serviceName;
    
    @Indexed
    private String operation;
    
    @Indexed
    private String userId;
    
    private String userName;
    
    @Indexed
    private String logLevel; // INFO, WARN, ERROR
    
    private String methodName;
    
    private String className;
    
    private Map<String, Object> parameters;
    
    private Object result;
    
    private String errorMessage;
    
    private String stackTrace;
    
    private Long executionTimeMs;
    
    @Indexed
    private String traceId;
    
    private String spanId;
    
    private Map<String, String> metadata;
}
```

### AuditTrail.java

```java
package pain.management.logging.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "audit_trail")
public class AuditTrail {
    
    @Id
    private String id;
    
    @Indexed
    private LocalDateTime timestamp;
    
    @Indexed
    private String userId;
    
    private String userName;
    
    @Indexed
    private String action; // CREATE, UPDATE, DELETE, READ
    
    @Indexed
    private String entityType;
    
    private String entityId;
    
    private Map<String, Object> oldValue;
    
    private Map<String, Object> newValue;
    
    private String ipAddress;
    
    private String userAgent;
    
    @Indexed
    private String serviceName;
    
    private String description;
}
```

---

## 🗄️ Repository

### LogEntryRepository.java

```java
package pain.management.logging.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import pain.management.logging.entity.LogEntry;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface LogEntryRepository extends MongoRepository<LogEntry, String> {
    
    List<LogEntry> findByUserIdAndTimestampBetween(
        String userId, 
        LocalDateTime from, 
        LocalDateTime to
    );
    
    List<LogEntry> findByServiceNameAndTimestampBetween(
        String serviceName, 
        LocalDateTime from, 
        LocalDateTime to
    );
    
    List<LogEntry> findByLogLevelAndTimestampBetween(
        String logLevel, 
        LocalDateTime from, 
        LocalDateTime to
    );
    
    List<LogEntry> findByTraceId(String traceId);
    
    void deleteByTimestampBefore(LocalDateTime cutoffDate);
}
```

---

## 🔧 Configuration

### application.yml

```yaml
server:
  port: 8081

spring:
  application:
    name: logging-service
  
  # MongoDB Configuration
  data:
    mongodb:
      uri: mongodb://admin:admin_password@localhost:27017/logging_db?authSource=admin
      auto-index-creation: true
  
  # Kafka Consumer Configuration
  kafka:
    bootstrap-servers: localhost:9092
    consumer:
      group-id: logging-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
      properties:
        spring.json.trusted.packages: "*"
        spring.json.type.mapping: logEvent:pain.management.logging.dto.LogEventDTO
    listener:
      ack-mode: manual
      concurrency: 3

# Eureka Client
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30

# Actuator
management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  endpoint:
    health:
      show-details: always
  metrics:
    export:
      prometheus:
        enabled: true
  tracing:
    sampling:
      probability: 1.0
  zipkin:
    tracing:
      endpoint: http://localhost:9411/api/v2/spans

# Logging
logging:
  level:
    pain.management.logging: DEBUG
    org.springframework.kafka: INFO
    org.mongodb: INFO

---
# Docker Profile
spring:
  config:
    activate:
      on-profile: docker
  data:
    mongodb:
      uri: mongodb://admin:admin_password@mongodb-logging:27017/logging_db?authSource=admin
  kafka:
    bootstrap-servers: kafka:9092

eureka:
  client:
    service-url:
      defaultZone: http://eureka-server:8761/eureka/

management:
  zipkin:
    tracing:
      endpoint: http://zipkin:9411/api/v2/spans
```

---

## 📡 Kafka Consumer

### KafkaConsumerConfig.java

```java
package pain.management.logging.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@EnableKafka
@Configuration
public class KafkaConsumerConfig {
    
    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;
    
    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3);
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
```

### LogEventConsumer.java

```java
package pain.management.logging.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import pain.management.logging.dto.LogEventDTO;
import pain.management.logging.service.LoggingService;

@Slf4j
@Component
@RequiredArgsConstructor
public class LogEventConsumer {
    
    private final LoggingService loggingService;
    
    @KafkaListener(
        topics = "${kafka.topic.logging-events:logging-events}",
        groupId = "${spring.kafka.consumer.group-id}",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void consumeLogEvent(
            @Payload LogEventDTO logEvent,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received log event from partition {} offset {}: {}", 
                     partition, offset, logEvent.getOperation());
            
            loggingService.processLogEvent(logEvent);
            
            acknowledgment.acknowledge();
            
            log.debug("Successfully processed log event: {}", logEvent.getOperation());
            
        } catch (Exception e) {
            log.error("Error processing log event: {}", logEvent, e);
            // Не acknowledge - сообщение будет обработано повторно
        }
    }
}
```

---

## 🔧 Service Layer

### LoggingService.java

```java
package pain.management.logging.service;

import pain.management.logging.dto.LogEventDTO;
import pain.management.logging.dto.LogQueryDTO;
import pain.management.logging.entity.LogEntry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;

public interface LoggingService {
    
    void processLogEvent(LogEventDTO logEvent);
    
    Page<LogEntry> searchLogs(LogQueryDTO query, Pageable pageable);
    
    List<LogEntry> getLogsByUserId(String userId, LocalDateTime from, LocalDateTime to);
    
    List<LogEntry> getLogsByService(String serviceName, LocalDateTime from, LocalDateTime to);
    
    List<LogEntry> getErrorLogs(LocalDateTime from, LocalDateTime to);
    
    LogEntry getLogById(String id);
    
    void deleteOldLogs(int daysToKeep);
}
```

### LoggingServiceImpl.java (см. следующий файл)

---

## 🐳 Dockerfile

```dockerfile
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY target/logging-service-1.0.0.jar app.jar
EXPOSE 8081
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s --retries=3 \
  CMD wget --no-verbose --tries=1 --spider http://localhost:8081/actuator/health || exit 1
ENTRYPOINT ["java", "-jar", "app.jar"]
```

---

**Следующий шаг:** [11_LOGGING_MONOLITH_MIGRATION.md](11_LOGGING_MONOLITH_MIGRATION.md)
