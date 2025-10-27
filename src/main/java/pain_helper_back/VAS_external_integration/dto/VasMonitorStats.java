package pain_helper_back.VAS_external_integration.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Статистика для VAS Monitor Dashboard.
 * 
 * ИСПОЛЬЗУЕТСЯ В:
 * - External VAS Monitor (Frontend)
 * - GET /api/external/vas/stats
 * 
 * РАСЧЕТ:
 * - Все метрики за текущий день (с 00:00)
 * - Только внешние источники (recordedBy LIKE 'EXTERNAL_%')
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VasMonitorStats {
    
    /**
     * Общее количество VAS записей за сегодня
     */
    private Integer totalRecordsToday;
    
    /**
     * Средний уровень боли за сегодня
     * Округлено до 1 знака после запятой
     */
    private Double averageVas;
    
    /**
     * Количество записей с высоким уровнем боли (VAS >= 7)
     */
    private Integer highPainAlerts;
    
    /**
     * Количество активных устройств за сегодня
     * COUNT DISTINCT deviceId
     */
    private Integer activeDevices;
}
