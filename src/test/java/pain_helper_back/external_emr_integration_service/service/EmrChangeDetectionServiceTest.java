package pain_helper_back.external_emr_integration_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.external_emr_integration_service.dto.EmrChangeAlertDTO;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/*
 * Тесты для EmrChangeDetectionService.
 * 
 * ПРОВЕРЯЕМАЯ ФУНКЦИОНАЛЬНОСТЬ:
 * - Обнаружение изменений в EMR данных
 * - Генерация алертов при критических изменениях
 * - Правильность определения уровней критичности
 * - Корректность пороговых значений
 */
@DisplayName("EmrChangeDetectionService Tests")
class EmrChangeDetectionServiceTest {

    private EmrChangeDetectionService service;

    @BeforeEach
    void setUp() {
        service = new EmrChangeDetectionService();
    }

    @Test
    @DisplayName("Должен обнаружить изменение GFR")
    void shouldDetectGfrChange() {
        // Given
        Emr oldEmr = createEmr("45", 200.0, 7.0, 140.0, 98.0);
        Emr newEmr = createEmr("30", 200.0, 7.0, 140.0, 98.0);

        // When
        boolean hasChanges = service.detectChanges(oldEmr, newEmr);

        // Then
        assertTrue(hasChanges, "Должно быть обнаружено изменение GFR");
    }

    @Test
    @DisplayName("Не должен обнаружить изменения если EMR идентичны")
    void shouldNotDetectChangesWhenEmrIdentical() {
        // Given
        Emr oldEmr = createEmr("45", 200.0, 7.0, 140.0, 98.0);
        Emr newEmr = createEmr("45", 200.0, 7.0, 140.0, 98.0);

        // When
        boolean hasChanges = service.detectChanges(oldEmr, newEmr);

        // Then
        assertFalse(hasChanges, "Не должно быть обнаружено изменений");
    }

    @Test
    @DisplayName("Должен сгенерировать CRITICAL алерт при GFR < 30")
    void shouldGenerateCriticalAlertForLowGfr() {
        // Given
        Emr oldEmr = createEmr("45", 200.0, 7.0, 140.0, 98.0);
        Emr newEmr = createEmr("25", 200.0, 7.0, 140.0, 98.0);

        // When
        List<EmrChangeAlertDTO> alerts = service.checkCriticalChanges(oldEmr, newEmr, "EMR-12345678");

        // Then
        assertFalse(alerts.isEmpty(), "Должен быть сгенерирован алерт");
        EmrChangeAlertDTO alert = alerts.get(0);
        assertEquals("GFR", alert.getParameterName());
        assertEquals(EmrChangeAlertDTO.AlertSeverity.CRITICAL, alert.getSeverity());
        assertTrue(alert.isRequiresRecommendationReview());
    }

    @Test
    @DisplayName("Должен сгенерировать CRITICAL алерт при PLT < 50")
    void shouldGenerateCriticalAlertForLowPlt() {
        // Given
        Emr oldEmr = createEmr("45", 80.0, 7.0, 140.0, 98.0);
        Emr newEmr = createEmr("45", 40.0, 7.0, 140.0, 98.0);

        // When
        List<EmrChangeAlertDTO> alerts = service.checkCriticalChanges(oldEmr, newEmr, "EMR-12345678");

        // Then
        assertFalse(alerts.isEmpty(), "Должен быть сгенерирован алерт");
        EmrChangeAlertDTO alert = alerts.get(0);
        assertEquals("PLT", alert.getParameterName());
        assertEquals(EmrChangeAlertDTO.AlertSeverity.CRITICAL, alert.getSeverity());
        assertTrue(alert.getChangeDescription().contains("тромбоциты"));
    }

    @Test
    @DisplayName("Должен сгенерировать CRITICAL алерт при WBC < 2.0")
    void shouldGenerateCriticalAlertForLowWbc() {
        // Given
        Emr oldEmr = createEmr("45", 200.0, 5.0, 140.0, 98.0);
        Emr newEmr = createEmr("45", 200.0, 1.5, 140.0, 98.0);

        // When
        List<EmrChangeAlertDTO> alerts = service.checkCriticalChanges(oldEmr, newEmr, "EMR-12345678");

        // Then
        assertFalse(alerts.isEmpty(), "Должен быть сгенерирован алерт");
        EmrChangeAlertDTO alert = alerts.get(0);
        assertEquals("WBC", alert.getParameterName());
        assertEquals(EmrChangeAlertDTO.AlertSeverity.CRITICAL, alert.getSeverity());
        assertTrue(alert.getChangeDescription().contains("лейкоциты"));
    }

    @Test
    @DisplayName("Должен сгенерировать CRITICAL алерт при SAT < 90")
    void shouldGenerateCriticalAlertForLowSat() {
        // Given
        Emr oldEmr = createEmr("45", 200.0, 7.0, 140.0, 95.0);
        Emr newEmr = createEmr("45", 200.0, 7.0, 140.0, 85.0);

        // When
        List<EmrChangeAlertDTO> alerts = service.checkCriticalChanges(oldEmr, newEmr, "EMR-12345678");

        // Then
        assertFalse(alerts.isEmpty(), "Должен быть сгенерирован алерт");
        EmrChangeAlertDTO alert = alerts.get(0);
        assertEquals("SAT", alert.getParameterName());
        assertEquals(EmrChangeAlertDTO.AlertSeverity.CRITICAL, alert.getSeverity());
        assertTrue(alert.getChangeDescription().contains("гипоксия"));
    }

    @Test
    @DisplayName("Не должен генерировать алерты при нормальных значениях")
    void shouldNotGenerateAlertsForNormalValues() {
        // Given
        Emr oldEmr = createEmr("60", 200.0, 7.0, 140.0, 98.0);
        Emr newEmr = createEmr("58", 195.0, 6.8, 139.0, 97.0);

        // When
        List<EmrChangeAlertDTO> alerts = service.checkCriticalChanges(oldEmr, newEmr, "EMR-12345678");

        // Then
        assertTrue(alerts.isEmpty(), "Не должно быть алертов при нормальных значениях");
    }

    @Test
    @DisplayName("Должен сгенерировать несколько алертов при множественных критических изменениях")
    void shouldGenerateMultipleAlertsForMultipleCriticalChanges() {
        // Given
        Emr oldEmr = createEmr("45", 80.0, 5.0, 140.0, 95.0);
        Emr newEmr = createEmr("25", 40.0, 1.5, 140.0, 85.0);

        // When
        List<EmrChangeAlertDTO> alerts = service.checkCriticalChanges(oldEmr, newEmr, "EMR-12345678");

        // Then
        assertEquals(4, alerts.size(), "Должно быть 4 алерта (GFR, PLT, WBC, SAT)");
        
        // Проверяем, что все алерты CRITICAL
        alerts.forEach(alert -> 
            assertEquals(EmrChangeAlertDTO.AlertSeverity.CRITICAL, alert.getSeverity())
        );
    }

    /**
     * Вспомогательный метод для создания EMR с заданными параметрами.
     */
    private Emr createEmr(String gfr, Double plt, Double wbc, Double sodium, Double sat) {
        Emr emr = new Emr();
        emr.setGfr(gfr);
        emr.setPlt(plt);
        emr.setWbc(wbc);
        emr.setSodium(sodium);
        emr.setSat(sat);
        emr.setWeight(70.0);
        emr.setHeight(175.0);
        emr.setChildPughScore("A");
        return emr;
    }
}
