package pain_helper_back.pain_escalation_tracking.controller;


import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.pain_escalation_tracking.dto.DoseAdministrationRequestDTO;
import pain_helper_back.pain_escalation_tracking.dto.DoseAdministrationResponseDTO;
import pain_helper_back.pain_escalation_tracking.dto.PainEscalationCheckResultDTO;
import pain_helper_back.pain_escalation_tracking.dto.PainTrendAnalysisDTO;
import pain_helper_back.pain_escalation_tracking.service.PainEscalationService;

import java.util.List;

@RestController
@RequestMapping("/api/pain-escalation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PainEscalationController {

    private final PainEscalationService painEscalationService;

    /**
     * Зарегистрировать факт введения новой дозы препарата
     */
    @PostMapping("/patients/{mrn}/administer-dose")
    @ResponseStatus(HttpStatus.CREATED)
    public DoseAdministrationResponseDTO registerDose(
            @PathVariable String mrn,
            @Valid @RequestBody DoseAdministrationRequestDTO request
    ) {
        return painEscalationService.registerDoseAdministration(mrn, request);
    }

    /**
     * Проверить, можно ли вводить следующую дозу (для отображения таймера/подсказки в UI)
     */
    @GetMapping("/patients/{mrn}/can-administer-next-dose")
    public DoseEligibilityDTO canAdministerNextDose(@PathVariable String mrn) {
        return painEscalationService.buildDoseEligibility(mrn);
    }

    /**
     * Получить анализ тренда боли за последние часы
     */
    @GetMapping("/patients/{mrn}/trend")
    public PainTrendAnalysisDTO getPainTrend(@PathVariable String mrn) {
        return painEscalationService.analyzePainTrend(mrn);
    }

    /**
     * Принудительно запустить проверку эскалации боли (например, с override текущего VAS)
     */
    @PostMapping("/patients/{mrn}/check")
    public PainEscalationCheckResultDTO checkEscalation(
            @PathVariable String mrn,
            @RequestBody(required = false) PainEscalationCheckCommand command
    ) {
        return painEscalationService.checkPainEscalation(mrn, command);
    }

    /**
     * Получить список активных эскалаций для отображения на dashboard
     */
    @GetMapping("/escalations/recent")
    public List<Escalation> getRecentEscalations(@RequestParam(defaultValue = "10") int limit) {
        return painEscalationService.findRecentEscalations(limit);
    }

    /**
     * Получить полную информацию об эскалации
     */
    @GetMapping("/escalations/{id}")
    public Escalation getEscalation(@PathVariable Long id) {
        return painEscalationService.findEscalationById(id);
    }

    /**
     * DTO с информацией о доступности следующей дозы
     */
    public record DoseEligibilityDTO(
            String patientMrn,
            boolean canAdminister,
            Long hoursSinceLastDose,
            Integer requiredInterval,
            String message
    ) { }

    /**
     * Команда для принудительной проверки эскалации боли
     */
    public record PainEscalationCheckCommand(Integer vasLevelOverride) { }
}
