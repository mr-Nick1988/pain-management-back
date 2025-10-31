# 08 - Apache Kafka Setup

**Предыдущий:** [07_API_GATEWAY.md](07_API_GATEWAY.md)  
**Следующий:** [09_LOGGING_SERVICE_OVERVIEW.md](09_LOGGING_SERVICE_OVERVIEW.md)

---

## 🎯 Зачем нужен Kafka

**Apache Kafka** - это распределенная платформа для потоковой обработки событий:

- **Асинхронная коммуникация** между микросервисами
- **Event Sourcing** - сохранение всех событий
- **Высокая пропускная способность** - миллионы сообщений в секунду
- **Надежность** - репликация и персистентность
- **Масштабируемость** - горизонтальное масштабирование

---

## 📊 Архитектура Kafka в проекте

```
┌─────────────────────────────────────────────────────────┐
│                    Producers                             │
├─────────────────────────────────────────────────────────┤
│  Monolith  │  Logging Service  │  Analytics Service    │
└──────┬──────────────┬────────────────────┬──────────────┘
       │              │                    │
       ▼              ▼                    ▼
┌─────────────────────────────────────────────────────────┐
│                   Apache Kafka                           │
├─────────────────────────────────────────────────────────┤
│  Topic: logging-events        (3 partitions)            │
│  Topic: analytics-events      (3 partitions)            │
│  Topic: escalation-events     (2 partitions)            │
│  Topic: emr-sync-events       (2 partitions)            │
│  Topic: notification-events   (2 partitions)            │
└──────┬──────────────┬────────────────────┬──────────────┘
       │              │                    │
       ▼              ▼                    ▼
┌─────────────────────────────────────────────────────────┐
│                    Consumers                             │
├─────────────────────────────────────────────────────────┤
│  Logging Service  │  Analytics Service  │  Notification │
└─────────────────────────────────────────────────────────┘
```

---

## 🐳 Docker Compose конфигурация

Уже добавлено в `docker-compose.yml` (см. [04_DOCKER_SETUP.md](04_DOCKER_SETUP.md)):

```yaml
zookeeper:
  image: confluentinc/cp-zookeeper:7.5.0
  ports:
    - "2181:2181"
  environment:
    ZOOKEEPER_CLIENT_PORT: 2181
    ZOOKEEPER_TICK_TIME: 2000

kafka:
  image: confluentinc/cp-kafka:7.5.0
  ports:
    - "9092:9092"
    - "29092:29092"
  environment:
    KAFKA_BROKER_ID: 1
    KAFKA_ZOOKEEPER_CONNECT: zookeeper:2181
    KAFKA_ADVERTISED_LISTENERS: PLAINTEXT://kafka:9092,PLAINTEXT_HOST://localhost:29092
    KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT
    KAFKA_INTER_BROKER_LISTENER_NAME: PLAINTEXT
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
    KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"
  depends_on:
    - zookeeper

kafka-ui:
  image: provectuslabs/kafka-ui:latest
  ports:
    - "8090:8080"
  environment:
    KAFKA_CLUSTERS_0_NAME: local
    KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:9092
  depends_on:
    - kafka
```

---

## 🚀 Запуск Kafka

```bash
# Запуск Zookeeper и Kafka
docker-compose up -d zookeeper kafka

# Проверка статуса
docker-compose ps

# Логи Kafka
docker-compose logs -f kafka

# Запуск Kafka UI
docker-compose up -d kafka-ui
```

Kafka UI доступен по адресу: http://localhost:8090

---

## 📝 Создание топиков

### Автоматическое создание

Kafka настроен на автоматическое создание топиков (`KAFKA_AUTO_CREATE_TOPICS_ENABLE: "true"`).

### Ручное создание

```bash
# Войти в контейнер Kafka
docker exec -it kafka bash

# Создать топик
kafka-topics --create \
  --bootstrap-server localhost:9092 \
  --topic logging-events \
  --partitions 3 \
  --replication-factor 1

# Список топиков
kafka-topics --list --bootstrap-server localhost:9092

# Описание топика
kafka-topics --describe \
  --bootstrap-server localhost:9092 \
  --topic logging-events
```

### Скрипт создания всех топиков

Создайте файл `create-topics.sh`:

```bash
#!/bin/bash

KAFKA_CONTAINER="kafka"
BOOTSTRAP_SERVER="localhost:9092"

echo "Creating Kafka topics..."

# Logging events
docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server $BOOTSTRAP_SERVER \
  --topic logging-events \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

# Analytics events
docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server $BOOTSTRAP_SERVER \
  --topic analytics-events \
  --partitions 3 \
  --replication-factor 1 \
  --if-not-exists

# Escalation events
docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server $BOOTSTRAP_SERVER \
  --topic escalation-events \
  --partitions 2 \
  --replication-factor 1 \
  --if-not-exists

# EMR sync events
docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server $BOOTSTRAP_SERVER \
  --topic emr-sync-events \
  --partitions 2 \
  --replication-factor 1 \
  --if-not-exists

# Notification events
docker exec $KAFKA_CONTAINER kafka-topics --create \
  --bootstrap-server $BOOTSTRAP_SERVER \
  --topic notification-events \
  --partitions 2 \
  --replication-factor 1 \
  --if-not-exists

echo "Topics created successfully!"

# List all topics
docker exec $KAFKA_CONTAINER kafka-topics --list --bootstrap-server $BOOTSTRAP_SERVER
```

Запуск:

```bash
chmod +x create-topics.sh
./create-topics.sh
```

---

## 🔧 Конфигурация Producer

### Maven зависимость

```xml
<dependency>
    <groupId>org.springframework.kafka</groupId>
    <artifactId>spring-kafka</artifactId>
</dependency>
```

### KafkaProducerConfig.java

```java
package pain.management.config;

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
        return new DefaultKafkaProducerFactory<>(config);
    }
    
    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }
}
```

### Отправка сообщения

```java
@Service
@RequiredArgsConstructor
public class EventPublisher {
    
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    public void publishLogEvent(LogEvent event) {
        kafkaTemplate.send("logging-events", event.getId(), event);
    }
}
```

---

## 🔧 Конфигурация Consumer

### KafkaConsumerConfig.java

```java
package pain.management.config;

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
    
    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
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

### Получение сообщения

```java
@Component
@RequiredArgsConstructor
@Slf4j
public class LogEventConsumer {
    
    private final LoggingService loggingService;
    
    @KafkaListener(
        topics = "logging-events",
        groupId = "${spring.kafka.consumer.group-id}"
    )
    public void consumeLogEvent(
            @Payload LogEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment
    ) {
        try {
            log.debug("Received event from partition {} offset {}", partition, offset);
            loggingService.processLogEvent(event);
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing event", e);
            // Не acknowledge - сообщение будет обработано повторно
        }
    }
}
```

---

## 🔍 Тестирование Kafka

### Отправка тестового сообщения

```bash
# Войти в контейнер
docker exec -it kafka bash

# Отправить сообщение
echo '{"id":"test-1","message":"Hello Kafka"}' | \
  kafka-console-producer \
    --bootstrap-server localhost:9092 \
    --topic logging-events
```

### Чтение сообщений

```bash
# Читать с начала
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic logging-events \
  --from-beginning

# Читать только новые
kafka-console-consumer \
  --bootstrap-server localhost:9092 \
  --topic logging-events
```

---

## 📊 Мониторинг Kafka

### Kafka UI

Откройте http://localhost:8090

Вы увидите:
- Список топиков
- Количество сообщений
- Consumer groups
- Brokers
- Partitions

### Метрики через JMX

```bash
# Включить JMX в docker-compose.yml
environment:
  KAFKA_JMX_PORT: 9999
  KAFKA_JMX_HOSTNAME: localhost

ports:
  - "9999:9999"
```

### Consumer Lag

```bash
# Проверить lag consumer group
docker exec kafka kafka-consumer-groups \
  --bootstrap-server localhost:9092 \
  --describe \
  --group logging-service-group
```

---

## ⚙️ Production настройки

### Репликация

```yaml
kafka:
  environment:
    KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 3
    KAFKA_TRANSACTION_STATE_LOG_REPLICATION_FACTOR: 3
    KAFKA_TRANSACTION_STATE_LOG_MIN_ISR: 2
```

### Retention

```yaml
kafka:
  environment:
    KAFKA_LOG_RETENTION_HOURS: 168  # 7 дней
    KAFKA_LOG_RETENTION_BYTES: 1073741824  # 1 GB
    KAFKA_LOG_SEGMENT_BYTES: 1073741824
```

### Performance

```yaml
kafka:
  environment:
    KAFKA_NUM_NETWORK_THREADS: 8
    KAFKA_NUM_IO_THREADS: 8
    KAFKA_SOCKET_SEND_BUFFER_BYTES: 102400
    KAFKA_SOCKET_RECEIVE_BUFFER_BYTES: 102400
    KAFKA_SOCKET_REQUEST_MAX_BYTES: 104857600
```

---

## ✅ Чеклист

- [ ] Zookeeper запущен
- [ ] Kafka запущен
- [ ] Kafka UI доступен
- [ ] Топики созданы
- [ ] Producer конфигурация готова
- [ ] Consumer конфигурация готова
- [ ] Тестовые сообщения отправлены и получены

---

**Следующий шаг:** [09_LOGGING_SERVICE_OVERVIEW.md](09_LOGGING_SERVICE_OVERVIEW.md)
