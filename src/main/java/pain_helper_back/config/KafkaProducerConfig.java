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
import pain_helper_back.kafka.dto.AnalyticsEventDTO;
import pain_helper_back.kafka.dto.ReportingCommand;

import java.util.HashMap;
import java.util.Map;

/**
 * Kafka Producer configuration for sending commands and events to microservices
 */
@Configuration
public class KafkaProducerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Producer factory for ReportingCommand
     */
    @Bean
    public ProducerFactory<String, ReportingCommand> reportingCommandProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for ReportingCommand
     */
    @Bean
    public KafkaTemplate<String, ReportingCommand> reportingCommandKafkaTemplate() {
        return new KafkaTemplate<>(reportingCommandProducerFactory());
    }

    /**
     * Producer factory for AnalyticsEventDTO
     */
    @Bean
    public ProducerFactory<String, AnalyticsEventDTO> analyticsEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        configProps.put(ProducerConfig.ACKS_CONFIG, "1"); // Analytics events don't need full replication
        configProps.put(ProducerConfig.RETRIES_CONFIG, 1); // Fewer retries for analytics
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, false); // Not critical for analytics
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * KafkaTemplate for AnalyticsEventDTO
     */
    @Bean
    public KafkaTemplate<String, AnalyticsEventDTO> analyticsEventKafkaTemplate() {
        return new KafkaTemplate<>(analyticsEventProducerFactory());
    }
}
