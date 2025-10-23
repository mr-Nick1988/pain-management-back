package pain_helper_back.pain_escalation_tracking.controller;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.pain_escalation_tracking.dto.*;
import pain_helper_back.pain_escalation_tracking.service.PainEscalationService;

import jakarta.validation.Valid;
import java.util.List;

/**
 * REST API для управления дозами и эскалацией боли.
 * Позволяет медсестрам регистрировать введенные дозы и проверять возможность следующей дозы.
 */
@RestController
@RequestMapping("/api/pain-escalation")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class DoseAdministrationController {

    private final PainEscalationService painEscalationService;
    /*
     * Регистрация введенной дозы препарата.
     *
     * @param mrn MRN пациента
     * @param request данные о введенной дозе
     * @return результат регистрации
     */
    @PostMapping("/patients/{mrn}/doses")
    public ResponseEntity<DoseAdministrationResponseDTO> registerDose(
            @PathVariable String mrn,
            @Valid @RequestBody DoseAdministrationRequestDTO request) {

        log.info("Registering dose for patient {}: {}  of {}",
                mrn, request.getDosage(), request.getDrugName());

        var result = painEscalationService.registerDoseAdministration(mrn, request);

        var response = DoseAdministrationResponseDTO.builder()
                .success(true)
                .message("Dose registered successfully")
                .doseId(result.getDoseId())
                .administeredAt(result.getAdministeredAt())
                .nextDoseAllowedAt(result.getNextDoseAllowedAt())
                .build();

        log.info("Dose registered successfully for patient {}", mrn);
        return ResponseEntity.ok(response);
    }
    /*
     * Проверка возможности введения следующей дозы.
     *
     * @param mrn MRN пациента
     * @return информация о возможности введения дозы
     */
    @GetMapping("/patients/{mrn}/can-administer-dose")
    public ResponseEntity<CanAdministerDoseResponseDTO> canAdministerDose(@PathVariable String mrn) {

        log.info("Checking if next dose can be administered for patient {}", mrn);

        boolean canAdminister = painEscalationService.canAdministerNextDose(mrn);

        var response = CanAdministerDoseResponseDTO.builder()
                .canAdminister(canAdminister)
                .patientMrn(mrn)
                .message(canAdminister
                        ? "Next dose can be administered"
                        : "Minimum interval not met - wait before next dose")
                .build();

        return ResponseEntity.ok(response);
    }
    /*
     * Получить историю доз пациента.
     *
     * @param mrn MRN пациента
     * @return список введенных доз
     */
    @GetMapping("/patients/{mrn}/doses")
    public ResponseEntity<List<DoseHistoryDTO>> getDoseHistory(@PathVariable String mrn) {

        log.info("Fetching dose history for patient {}", mrn);

        var doses = painEscalationService.getDoseHistory(mrn);

        return ResponseEntity.ok(doses);
    }
    /*
     * Проверка эскалации боли для пациента.
     *
     * @param mrn MRN пациента
     * @return результат проверки эскалации
     */
    @PostMapping("/patients/{mrn}/check-escalation")
    public ResponseEntity<PainEscalationCheckResultDTO> checkPainEscalation(@PathVariable String mrn) {

        log.info("Checking pain escalation for patient {}", mrn);

        var result = painEscalationService.checkPainEscalation(mrn);

        return ResponseEntity.ok(result);
    }
    /*
     * Получить последнюю эскалацию пациента.
     *
     * @param mrn MRN пациента
     * @return информация об эскалации
     */
    @GetMapping("/patients/{mrn}/latest-escalation")
    public ResponseEntity<EscalationInfoDTO> getLatestEscalation(@PathVariable String mrn) {

        log.info("Fetching latest escalation for patient {}", mrn);

        var escalation = painEscalationService.getLatestEscalation(mrn);

        if (escalation == null) {
            return ResponseEntity.noContent().build();
        }

        var response = EscalationInfoDTO.builder()
                .escalationId(escalation.getId())
                .patientMrn(mrn)
                .priority(escalation.getPriority())
                .status(escalation.getStatus())
                .reason(escalation.getEscalationReason())
                .createdAt(escalation.getCreatedAt())
                .build();

        return ResponseEntity.ok(response);
    }
    /*
     * Получить статистику по эскалациям.
     *
     * @return статистика
     */
    @GetMapping("/statistics")
    public ResponseEntity<PainEscalationStatisticsDTO> getStatistics() {

        log.info("Fetching pain escalation statistics");

        var stats = painEscalationService.getEscalationStatistics();

        return ResponseEntity.ok(stats);
    }
}