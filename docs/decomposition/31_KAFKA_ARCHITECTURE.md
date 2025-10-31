# 31 - Kafka Architecture

**Предыдущий:** [30_BACKUP_SERVICE_IMPLEMENTATION.md](30_BACKUP_SERVICE_IMPLEMENTATION.md)  
**Следующий:** [32_KAFKA_TOPICS_DESIGN.md](32_KAFKA_TOPICS_DESIGN.md)

---

## 🎯 Роль Kafka в архитектуре

Apache Kafka - это **центральная нервная система** микросервисной архитектуры, обеспечивающая:

- **Асинхронную коммуникацию** между сервисами
- **Event Sourcing** - сохранение всех событий
- **Decoupling** - слабая связанность сервисов
- **Scalability** - горизонтальное масштабирование
- **Reliability** - гарантированная доставка

---

## 📊 Топология Kafka

```
┌─────────────────────────────────────────────────────────┐
│                   Kafka Cluster                          │
├─────────────────────────────────────────────────────────┤
│  Broker 1 (Leader)    │  Broker 2 (Follower)            │
│  - Topic: logging     │  - Topic: logging (replica)     │
│  - Topic: analytics   │  - Topic: analytics (replica)   │
│  - Topic: escalation  │  - Topic: escalation (replica)  │
└─────────────────────────────────────────────────────────┘
```

---

## 📋 Список топиков

| Topic | Partitions | Replication | Producer | Consumer | Назначение |
|-------|------------|-------------|----------|----------|------------|
| **logging-events** | 3 | 1 (dev) / 3 (prod) | Monolith | Logging Service | Логирование операций |
| **analytics-events** | 3 | 1 / 3 | Logging Service | Analytics Service | Аналитические события |
| **escalation-events** | 2 | 1 / 3 | Pain Escalation | Notification Service | Эскалации боли |
| **emr-sync-events** | 2 | 1 / 3 | EMR Integration | Monolith | Синхронизация EMR |
| **notification-events** | 2 | 1 / 3 | Multiple | Notification Service | Уведомления |
| **audit-events** | 2 | 1 / 3 | Multiple | Logging Service | Audit trail |

---

## 🔄 Паттерны коммуникации

### 1. Event Notification

```
Producer → Kafka → Consumer(s)
```

**Пример:** Логирование операций

```java
// Producer (Monolith)
LogEvent event = new LogEvent(...);
kafkaTemplate.send("logging-events", event);

// Consumer (Logging Service)
@KafkaListener(topics = "logging-events")
public void handle(LogEvent event) {
    logEntryRepository.save(event);
}
```

### 2. Event-Carried State Transfer

```
Producer → Kafka → Consumer(s) (обновляют локальный кэш)
```

**Пример:** Синхронизация данных пациентов

```java
// Producer
PatientUpdatedEvent event = new PatientUpdatedEvent(patient);
kafkaTemplate.send("patient-events", event);

// Consumer (кэширует данные)
@KafkaListener(topics = "patient-events")
public void handle(PatientUpdatedEvent event) {
    patientCache.update(event.getPatient());
}
```

### 3. Event Sourcing

```
Commands → Events → Kafka → Event Store
```

**Пример:** История изменений протоколов

```java
// Command
CreateProtocolCommand cmd = new CreateProtocolCommand(...);

// Event
ProtocolCreatedEvent event = new ProtocolCreatedEvent(...);
kafkaTemplate.send("protocol-events", event);

// Event Store
@KafkaListener(topics = "protocol-events")
public void store(ProtocolCreatedEvent event) {
    eventStore.append(event);
}
```

---

## 🔧 Конфигурация Producer

### Базовая конфигурация

```java
@Configuration
public class KafkaProducerConfig {
    
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Основные настройки
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        config.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Надежность
        config.put(ProducerConfig.ACKS_CONFIG, "all"); // Ждать подтверждения от всех реплик
        config.put(ProducerConfig.RETRIES_CONFIG, Integer.MAX_VALUE);
        config.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 5);
        config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Производительность
        config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
        config.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        
        return new DefaultKafkaProducerFactory<>(config);
    }
}
```

---

## 🔧 Конфигурация Consumer

### Базовая конфигурация

```java
@Configuration
public class KafkaConsumerConfig {
    
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = new HashMap<>();
        
        // Основные настройки
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Надежность
        config.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false); // Manual commit
        config.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        config.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        // Производительность
        config.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        config.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        config.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 500);
        
        return new DefaultKafkaConsumerFactory<>(config);
    }
    
    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
            new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setConcurrency(3); // 3 consumer threads
        factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
        return factory;
    }
}
```

---

## 📊 Мониторинг Kafka

### Ключевые метрики

#### Producer Metrics

```
kafka.producer.record-send-rate
kafka.producer.record-error-rate
kafka.producer.request-latency-avg
kafka.producer.buffer-available-bytes
```

#### Consumer Metrics

```
kafka.consumer.records-consumed-rate
kafka.consumer.records-lag
kafka.consumer.fetch-latency-avg
kafka.consumer.commit-latency-avg
```

### Проверка через CLI

```bash
# Consumer lag
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group logging-service-group

# Topic details
docker exec kafka kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic logging-events
```

---

## 🛡️ Обработка ошибок

### Retry Pattern

```java
@KafkaListener(topics = "logging-events")
public void consume(LogEvent event, Acknowledgment ack) {
    int maxRetries = 3;
    int attempt = 0;
    
    while (attempt < maxRetries) {
        try {
            processEvent(event);
            ack.acknowledge();
            return;
        } catch (Exception e) {
            attempt++;
            if (attempt >= maxRetries) {
                sendToDeadLetterQueue(event, e);
                ack.acknowledge();
            } else {
                Thread.sleep(1000 * attempt); // Exponential backoff
            }
        }
    }
}
```

### Dead Letter Queue (DLQ)

```java
@Bean
public KafkaTemplate<String, Object> dlqKafkaTemplate() {
    return new KafkaTemplate<>(producerFactory());
}

private void sendToDeadLetterQueue(LogEvent event, Exception e) {
    DeadLetterEvent dlqEvent = DeadLetterEvent.builder()
        .originalEvent(event)
        .error(e.getMessage())
        .timestamp(LocalDateTime.now())
        .build();
    
    dlqKafkaTemplate.send("logging-events-dlq", dlqEvent);
}
```

---

## 🔐 Безопасность

### SSL/TLS (Production)

```properties
spring.kafka.security.protocol=SSL
spring.kafka.ssl.trust-store-location=/path/to/truststore.jks
spring.kafka.ssl.trust-store-password=${TRUSTSTORE_PASSWORD}
spring.kafka.ssl.key-store-location=/path/to/keystore.jks
spring.kafka.ssl.key-store-password=${KEYSTORE_PASSWORD}
```

### SASL Authentication

```properties
spring.kafka.security.protocol=SASL_SSL
spring.kafka.properties.sasl.mechanism=PLAIN
spring.kafka.properties.sasl.jaas.config=org.apache.kafka.common.security.plain.PlainLoginModule required username="${KAFKA_USERNAME}" password="${KAFKA_PASSWORD}";
```

---

## 📈 Best Practices

### 1. Idempotent Producer

```java
config.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
```

### 2. Manual Commit

```java
factory.getContainerProperties().setAckMode(ContainerProperties.AckMode.MANUAL);
```

### 3. Compression

```java
config.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
```

### 4. Batch Processing

```java
config.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
config.put(ProducerConfig.LINGER_MS_CONFIG, 10);
```

### 5. Consumer Concurrency

```java
factory.setConcurrency(3); // По количеству партиций
```

---

**Следующий шаг:** [32_KAFKA_TOPICS_DESIGN.md](32_KAFKA_TOPICS_DESIGN.md)
