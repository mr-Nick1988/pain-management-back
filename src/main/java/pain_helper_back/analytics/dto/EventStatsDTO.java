package pain_helper_back.analytics.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EventStatsDTO {
    private Long totalEvents;
    private Map<String, Long> eventsByType;
    private Map<String, Long> eventsByRole;
    private Map<String, Long> eventsByStatus;
}
