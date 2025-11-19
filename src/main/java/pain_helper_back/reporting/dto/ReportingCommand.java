package pain_helper_back.reporting.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportingCommand {
    private String action;       // GENERATE_DAILY | GENERATE_YESTERDAY | GENERATE_PERIOD
    private LocalDate date;      // for GENERATE_DAILY
    private Boolean regenerate;  // default = false
    private LocalDate startDate; // for GENERATE_PERIOD
    private LocalDate endDate;   // for GENERATE_PERIOD
}
