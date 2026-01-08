package pain_helper_back.emr_recalculation.listener;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.emr_recalculation.dto.EmrChangeAlertDTO;
import pain_helper_back.emr_recalculation.service.EmrRecalculationService;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmrEventListener {

    private final EmrRecalculationService emrRecalculationService;
    private final PatientRepository patientRepository;
    private final EmrRepository emrRepository;

    @KafkaListener(topics = "emr.changes", groupId = "pain-monolith-emr-group")
    public void handleEmrChanges(List<Map<String, Object>> rawAlerts) {
        // Note: Ideally we deserialize directly to List<EmrChangeAlertDTO>, but JsonDeserializer with List can be tricky.
        // Assuming the payload comes as a List of objects.
        // We need to extract the MRN from the alerts.
        
        if (rawAlerts == null || rawAlerts.isEmpty()) {
            return;
        }

        try {
            // Convert raw map to DTO (simplified for robustness)
            // In a real scenario, use ObjectMapper or proper TypeReference in Deserializer
            // Taking the MRN from the first alert
            Map<String, Object> firstAlert = rawAlerts.get(0);
            String mrn = (String) firstAlert.get("patientMrn");
            
            log.info("Received EMR change event for MRN: {}", mrn);

            Patient patient = patientRepository.findByMrn(mrn)
                    .orElseThrow(() -> new RuntimeException("Patient not found: " + mrn));

            List<Emr> emrs = emrRepository.findByPatientMrn(mrn);
            if (emrs.isEmpty()) {
                log.warn("No EMR records found for patient {}", mrn);
                return;
            }
            // Get latest EMR
            Emr latestEmr = emrs.get(emrs.size() - 1);

            // Manual mapping or usage of mapper would be better, but for now we trust the flow
            // Since we can't easily reconstruct DTOs from Map without ObjectMapper here, 
            // and we want to avoid heavy logic in listener.
            // Let's assume we can map it.
            
            // For now, we will trigger recalculation based on the fact that something changed.
            // The service actually needs the specific alerts to generate the reason.
            
            // TODO: Ensure proper deserialization in production
             List<EmrChangeAlertDTO> alerts = rawAlerts.stream()
                .map(this::mapToDto)
                .collect(Collectors.toList());

            emrRecalculationService.handleCriticalEmrChanges(patient, alerts, latestEmr);
            
        } catch (Exception e) {
            log.error("Error processing EMR event: {}", e.getMessage(), e);
        }
    }

    private EmrChangeAlertDTO mapToDto(Map<String, Object> map) {
        return EmrChangeAlertDTO.builder()
                .patientMrn((String) map.get("patientMrn"))
                .parameterName((String) map.get("parameterName"))
                .oldValue((String) map.get("oldValue"))
                .newValue((String) map.get("newValue"))
                .changeDescription((String) map.get("changeDescription"))
                .recommendation((String) map.get("recommendation"))
                .build();
    }
}
