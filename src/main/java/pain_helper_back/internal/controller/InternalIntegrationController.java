package pain_helper_back.internal.controller;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.repository.EmrRepository;
import pain_helper_back.common.patients.repository.PatientRepository;
import pain_helper_back.emr_recalculation.dto.EmrChangeAlertDTO;
import pain_helper_back.emr_recalculation.service.EmrRecalculationService;

import java.util.List;

@RestController
@RequestMapping("/api/internal/emr")
@RequiredArgsConstructor
@Slf4j
public class InternalIntegrationController {

    private final EmrRecalculationService emrRecalculationService;
    private final PatientRepository patientRepository;
    private final EmrRepository emrRepository;

    /**
     * Internal endpoint to receive EMR change notifications from External EMR Microservice.
     */
    @PostMapping("/changes/{mrn}")
    public ResponseEntity<Void> handleEmrChanges(
            @PathVariable String mrn,
            @RequestBody List<EmrChangeAlertDTO> alerts) {
        
        log.info("Received EMR change notification for MRN: {} with {} alerts", mrn, alerts.size());

        Patient patient = patientRepository.findByMrn(mrn)
                .orElseThrow(() -> new RuntimeException("Patient not found: " + mrn));

        // Assuming shared DB for now, fetch the latest EMR
        List<Emr> emrs = emrRepository.findByPatientMrn(mrn);
        if (emrs.isEmpty()) {
            log.error("No EMR records found for patient {}", mrn);
            return ResponseEntity.notFound().build();
        }
        Emr latestEmr = emrs.get(0); // Assuming ordered by desc

        emrRecalculationService.handleCriticalEmrChanges(patient, alerts, latestEmr);

        return ResponseEntity.ok().build();
    }
}
