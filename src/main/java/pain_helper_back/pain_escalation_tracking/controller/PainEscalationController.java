package pain_helper_back.pain_escalation_tracking.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import pain_helper_back.pain_escalation_tracking.dto.PainTrendAnalysisDTO;
import pain_helper_back.pain_escalation_tracking.service.PainEscalationService;

/**
 * Контроллер для анализа и отслеживания роста боли (Pain Escalation Tracking)
 * Используется для проверки VAS, анализа трендов и автоматического реагирования системы.
 */
@RestController
@RequestMapping("/api/pain-escalation")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class PainEscalationController {

    private final PainEscalationService painEscalationService;

    /**
     * Анализ тренда боли за последние 24 ч для графиков и аналитики
     */
    @GetMapping("/patients/{mrn}/trend")
    public PainTrendAnalysisDTO getPainTrend(@PathVariable String mrn) {
        return painEscalationService.analyzePainTrend(mrn);
    }

    /**
     * Реакция на новую жалобу пациента (новое VAS)
     * Если боль выросла ≥ 2 баллов — уведомляем анестезиолога
     */
    //TODO пока не используется но может пригодится для VAS external service
    @PostMapping("/patients/{mrn}/new-vas")
    @ResponseStatus(HttpStatus.CREATED)
    public void handleNewVasRecord(
            @PathVariable String mrn,
            @Valid @RequestBody NewVasRecordCommand command
    ) {
        painEscalationService.handleNewVasRecord(mrn, command.newVasLevel());
    }

    /**
     * Команда поступления новой записи VAS
     * record — это специальный тип класса, появившийся в Java 16.
     * Он нужен, чтобы быстро описывать неизменяемые DTO (создание на ходу),
     * где всё, что тебе нужно — это хранить данные.
     */
    public record NewVasRecordCommand(Integer newVasLevel) {
    }
}
