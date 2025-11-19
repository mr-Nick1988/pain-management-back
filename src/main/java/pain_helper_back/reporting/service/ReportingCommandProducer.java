package pain_helper_back.reporting.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import pain_helper_back.reporting.dto.ReportingCommand;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReportingCommandProducer {

    @Qualifier("reportingKafkaTemplate")
    private final KafkaTemplate<String, ReportingCommand> kafka;

    @Value("${kafka.topics.reporting-commands:reporting-commands}")
    private String topic;

    public void sendGenerateDaily(LocalDate date, boolean regenerate) {
        ReportingCommand cmd = ReportingCommand.builder()
                .action("GENERATE_DAILY")
                .date(date)
                .regenerate(regenerate)
                .build();
        kafka.send(topic, date.toString(), cmd);
        log.info("Reporting command published: action={}, date={}, regenerate={}", cmd.getAction(), date, regenerate);
    }

    public void sendGenerateYesterday(boolean regenerate) {
        ReportingCommand cmd = ReportingCommand.builder()
                .action("GENERATE_YESTERDAY")
                .regenerate(regenerate)
                .build();
        kafka.send(topic, "YESTERDAY", cmd);
        log.info("Reporting command published: action={}, regenerate={}", cmd.getAction(), regenerate);
    }

    public void sendGeneratePeriod(LocalDate start, LocalDate end, boolean regenerate) {
        ReportingCommand cmd = ReportingCommand.builder()
                .action("GENERATE_PERIOD")
                .startDate(start)
                .endDate(end)
                .regenerate(regenerate)
                .build();
        kafka.send(topic, start + "_" + end, cmd);
        log.info("Reporting command published: action={}, start={}, end={}, regenerate={}", cmd.getAction(), start, end, regenerate);
    }
}
