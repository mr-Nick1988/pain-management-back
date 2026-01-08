package pain_helper_back.kafka.producer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;
import pain_helper_back.kafka.dto.ReportingCommand;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Kafka producer for publishing reporting commands
 * Topic: reporting-commands
 * Consumer: Reporting Service
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportingCommandProducer {

    private final KafkaTemplate<String, ReportingCommand> kafkaTemplate;

    @Value("${kafka.topic.reporting-commands:reporting-commands}")
    private String reportingCommandsTopic;

    /**
     * Publish command to generate daily report
     *
     * @param reportDate date for report generation
     * @param regenerate whether to regenerate if exists
     */
    public void publishGenerateDailyCommand(LocalDate reportDate, boolean regenerate) {
        ReportingCommand command = ReportingCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .commandType(ReportingCommand.ReportingCommandType.GENERATE_DAILY)
                .timestamp(LocalDateTime.now())
                .payload(ReportingCommand.ReportingCommandPayload.builder()
                        .reportDate(reportDate)
                        .regenerate(regenerate)
                        .build())
                .build();

        publishCommand(command);
    }

    /**
     * Publish command to generate yesterday's report
     */
    public void publishGenerateYesterdayCommand() {
        ReportingCommand command = ReportingCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .commandType(ReportingCommand.ReportingCommandType.GENERATE_YESTERDAY)
                .timestamp(LocalDateTime.now())
                .payload(ReportingCommand.ReportingCommandPayload.builder()
                        .regenerate(false)
                        .build())
                .build();

        publishCommand(command);
    }

    /**
     * Publish command to generate reports for period
     *
     * @param startDate period start date
     * @param endDate period end date
     * @param regenerate whether to regenerate if exists
     */
    public void publishGeneratePeriodCommand(LocalDate startDate, LocalDate endDate, boolean regenerate) {
        ReportingCommand command = ReportingCommand.builder()
                .commandId(UUID.randomUUID().toString())
                .commandType(ReportingCommand.ReportingCommandType.GENERATE_PERIOD)
                .timestamp(LocalDateTime.now())
                .payload(ReportingCommand.ReportingCommandPayload.builder()
                        .startDate(startDate)
                        .endDate(endDate)
                        .regenerate(regenerate)
                        .build())
                .build();

        publishCommand(command);
    }

    /**
     * Publish command to Kafka
     */
    private void publishCommand(ReportingCommand command) {
        try {
            log.info("Publishing reporting command: type={}, commandId={}", 
                    command.getCommandType(), command.getCommandId());

            CompletableFuture<SendResult<String, ReportingCommand>> future = 
                    kafkaTemplate.send(reportingCommandsTopic, command.getCommandId(), command);

            future.whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Successfully published command {} to topic {} at offset {}", 
                            command.getCommandId(), 
                            reportingCommandsTopic,
                            result.getRecordMetadata().offset());
                } else {
                    log.error("Failed to publish command {}: {}", 
                            command.getCommandId(), ex.getMessage(), ex);
                }
            });

        } catch (Exception e) {
            log.error("Error publishing reporting command: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish reporting command", e);
        }
    }
}
