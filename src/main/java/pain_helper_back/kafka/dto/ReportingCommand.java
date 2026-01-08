package pain_helper_back.kafka.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO for Kafka reporting commands
 * Published to topic: reporting-commands
 * Consumed by: Reporting Service
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportingCommand {

    /**
     * Unique command identifier
     */
    private String commandId;

    /**
     * Command type: GENERATE_DAILY, GENERATE_YESTERDAY, GENERATE_PERIOD
     */
    private ReportingCommandType commandType;

    /**
     * Timestamp when command was created
     */
    private LocalDateTime timestamp;

    /**
     * Command payload with parameters
     */
    private ReportingCommandPayload payload;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReportingCommandPayload {
        /**
         * Report date (for GENERATE_DAILY)
         */
        private LocalDate reportDate;

        /**
         * Start date (for GENERATE_PERIOD)
         */
        private LocalDate startDate;

        /**
         * End date (for GENERATE_PERIOD)
         */
        private LocalDate endDate;

        /**
         * Whether to regenerate if report already exists
         */
        private boolean regenerate;
    }

    public enum ReportingCommandType {
        GENERATE_DAILY,
        GENERATE_YESTERDAY,
        GENERATE_PERIOD
    }
}
