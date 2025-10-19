package pain_helper_back.external_emr_integration_service.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.enums.EmrSourceType;
import pain_helper_back.external_emr_integration_service.client.HapiFhirClient;
import pain_helper_back.external_emr_integration_service.dto.EmrChangeAlertDTO;
import pain_helper_back.external_emr_integration_service.dto.EmrSyncResultDTO;
import pain_helper_back.external_emr_integration_service.dto.FhirObservationDTO;
import pain_helper_back.external_emr_integration_service.entity.EmrMapping;
import pain_helper_back.external_emr_integration_service.repository.EmrMappingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/*
 * Тесты для EmrSyncScheduler.
 * 
 * ПРОВЕРЯЕМАЯ ФУНКЦИОНАЛЬНОСТЬ:
 * - Синхронизация всех FHIR пациентов
 * - Синхронизация одного пациента
 * - Обработка ошибок при синхронизации
 * - Генерация статистики синхронизации
 * - Отправка уведомлений при критических изменениях
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EmrSyncScheduler Tests")
class EmrSyncSchedulerTest {

    @Mock
    private EmrMappingRepository emrMappingRepository;

    @Mock
    private EmrRepository emrRepository;

    @Mock
    private HapiFhirClient hapiFhirClient;

    @Mock
    private EmrChangeDetectionService changeDetectionService;

    @Mock
    private WebSocketNotificationService webSocketNotificationService;

    @Mock
    private EmailNotificationService emailNotificationService;

    private EmrSyncScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new EmrSyncScheduler(
                emrMappingRepository,
                emrRepository,
                hapiFhirClient,
                changeDetectionService,
                webSocketNotificationService
        );
    }

    @Test
    @DisplayName("Должен успешно синхронизировать всех FHIR пациентов")
    void shouldSuccessfullySyncAllFhirPatients() {
        // Given
        List<EmrMapping> mappings = createMockMappings(2);
        when(emrMappingRepository.findBySourceType(EmrSourceType.FHIR_SERVER)).thenReturn(mappings);
        when(hapiFhirClient.getObservationsForPatient(anyString())).thenReturn(createMockObservations());
        when(emrRepository.findByPatientMrn(anyString())).thenReturn(List.of(createMockEmr()));
        when(changeDetectionService.detectChanges(any(), any())).thenReturn(true);
        when(changeDetectionService.checkCriticalChanges(any(), any(), anyString())).thenReturn(new ArrayList<>());

        // When
        EmrSyncResultDTO result = scheduler.syncAllFhirPatients();

        // Then
        assertNotNull(result);
        assertEquals(2, result.getTotalPatientsProcessed());
        assertTrue(result.getSuccessfulSyncs() > 0);
        assertNotNull(result.getSyncStartTime());
        assertNotNull(result.getSyncEndTime());
    }

    @Test
    @DisplayName("Должен вернуть пустой результат если нет FHIR пациентов")
    void shouldReturnEmptyResultWhenNoFhirPatients() {
        // Given
        when(emrMappingRepository.findBySourceType(EmrSourceType.FHIR_SERVER)).thenReturn(new ArrayList<>());

        // When
        EmrSyncResultDTO result = scheduler.syncAllFhirPatients();

        // Then
        assertNotNull(result);
        assertEquals(0, result.getTotalPatientsProcessed());
        assertEquals(0, result.getSuccessfulSyncs());
        assertEquals(0, result.getFailedSyncs());
    }

    @Test
    @DisplayName("Должен обработать ошибку при синхронизации пациента")
    void shouldHandleErrorDuringSyncSinglePatient() {
        // Given
        List<EmrMapping> mappings = createMockMappings(1);
        when(emrMappingRepository.findBySourceType(EmrSourceType.FHIR_SERVER)).thenReturn(mappings);
        when(hapiFhirClient.getObservationsForPatient(anyString()))
                .thenThrow(new RuntimeException("FHIR server unavailable"));

        // When
        EmrSyncResultDTO result = scheduler.syncAllFhirPatients();

        // Then
        assertNotNull(result);
        assertEquals(1, result.getTotalPatientsProcessed());
        assertEquals(1, result.getFailedSyncs());
        assertFalse(result.getErrorMessages().isEmpty());
    }

    @Test
    @DisplayName("Должен отправить WebSocket уведомления при критических алертах")
    void shouldSendWebSocketNotificationsForCriticalAlerts() {
        // Given
        List<EmrMapping> mappings = createMockMappings(1);
        when(emrMappingRepository.findBySourceType(EmrSourceType.FHIR_SERVER)).thenReturn(mappings);
        when(hapiFhirClient.getObservationsForPatient(anyString())).thenReturn(createMockObservations());
        when(emrRepository.findByPatientMrn(anyString())).thenReturn(List.of(createMockEmr()));
        when(changeDetectionService.detectChanges(any(), any())).thenReturn(true);
        
        // Создаем критический алерт
        List<EmrChangeAlertDTO> criticalAlerts = new ArrayList<>();
        EmrChangeAlertDTO alert =
                EmrChangeAlertDTO.builder()
                        .patientMrn("EMR-12345678")
                        .parameterName("GFR")
                        .severity(EmrChangeAlertDTO.AlertSeverity.CRITICAL)
                        .build();
        criticalAlerts.add(alert);
        
        when(changeDetectionService.checkCriticalChanges(any(), any(), anyString())).thenReturn(criticalAlerts);

        // When
        EmrSyncResultDTO result = scheduler.syncAllFhirPatients();

        // Then
        assertNotNull(result);
        assertFalse(result.getAlerts().isEmpty());
        verify(webSocketNotificationService, times(1)).sendCriticalAlerts(anyList());
    }

    /**
     * Вспомогательные методы для создания mock объектов
     */
    private List<EmrMapping> createMockMappings(int count) {
        List<EmrMapping> mappings = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            EmrMapping mapping = new EmrMapping();
            mapping.setExternalFhirId("Patient/" + i);
            mapping.setInternalEmrNumber("EMR-1234567" + i);
            mapping.setSourceType(EmrSourceType.FHIR_SERVER);
            mappings.add(mapping);
        }
        return mappings;
    }

    private List<FhirObservationDTO> createMockObservations() {
        List<FhirObservationDTO> observations = new ArrayList<>();
        
        FhirObservationDTO obs1 = new FhirObservationDTO();
        obs1.setLoincCode("2160-0");  // Креатинин
        obs1.setValue(1.2);
        observations.add(obs1);
        
        FhirObservationDTO obs2 = new FhirObservationDTO();
        obs2.setLoincCode("777-3");  // Тромбоциты
        obs2.setValue(200.0);
        observations.add(obs2);
        
        return observations;
    }

    private Emr createMockEmr() {
        Emr emr = new Emr();
        emr.setGfr("60");
        emr.setPlt(200.0);
        emr.setWbc(7.0);
        emr.setSodium(140.0);
        emr.setSat(98.0);
        return emr;
    }
}
