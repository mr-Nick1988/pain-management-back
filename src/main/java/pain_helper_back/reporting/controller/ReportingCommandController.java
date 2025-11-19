package pain_helper_back.reporting.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.reporting.service.ReportingCommandProducer;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/internal/reporting/commands")
@RequiredArgsConstructor
@Slf4j
public class ReportingCommandController {

    private final ReportingCommandProducer producer;

    @PostMapping("/generate/daily")
    public ResponseEntity<?> generateDaily(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "false") boolean regenerate
    ) {
        if (date == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "date is required"));
        }
        producer.sendGenerateDaily(date, regenerate);
        return ResponseEntity.accepted().body(Map.of(
                "status", "PUBLISHED",
                "action", "GENERATE_DAILY",
                "date", date,
                "regenerate", regenerate
        ));
    }

    @PostMapping("/generate/yesterday")
    public ResponseEntity<?> generateYesterday(
            @RequestParam(defaultValue = "false") boolean regenerate
    ) {
        producer.sendGenerateYesterday(regenerate);
        return ResponseEntity.accepted().body(Map.of(
                "status", "PUBLISHED",
                "action", "GENERATE_YESTERDAY",
                "regenerate", regenerate
        ));
    }

    @PostMapping("/generate/period")
    public ResponseEntity<?> generatePeriod(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(defaultValue = "false") boolean regenerate
    ) {
        if (startDate == null || endDate == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "startDate and endDate are required"));
        }
        if (endDate.isBefore(startDate)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "endDate must be greater than or equal to startDate"));
        }
        producer.sendGeneratePeriod(startDate, endDate, regenerate);
        return ResponseEntity.accepted().body(Map.of(
                "status", "PUBLISHED",
                "action", "GENERATE_PERIOD",
                "startDate", startDate,
                "endDate", endDate,
                "regenerate", regenerate
        ));
    }
}
