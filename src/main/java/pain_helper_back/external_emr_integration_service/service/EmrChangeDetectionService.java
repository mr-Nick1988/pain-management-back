package pain_helper_back.external_emr_integration_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pain_helper_back.common.patients.entity.Emr;
import pain_helper_back.external_emr_integration_service.dto.EmrChangeAlertDTO;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/*
 * Сервис для обнаружения критических изменений в EMR данных пациентов.
 * 
 * ОСНОВНЫЕ ФУНКЦИИ:
 * - Сравнение старых и новых значений лабораторных показателей
 * - Определение критичности изменений
 * - Генерация алертов для врачей
 * - Рекомендации по пересмотру назначений
 * 
 * КРИТИЧЕСКИЕ ПОРОГИ:
 * - GFR < 30: Тяжелая почечная недостаточность
 * - PLT < 50: Критическая тромбоцитопения
 * - WBC < 1.0: Тяжелая лейкопения
 * - Натрий < 125 или > 155: Опасный электролитный дисбаланс
 * - SAT < 90: Критическая гипоксия
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmrChangeDetectionService {

    
    /*
     * Обнаружить изменения между старым и новым EMR
     * 
     * @param oldEmr Старые данные EMR
     * @param newEmr Новые данные EMR
     * @return true если есть изменения, false если нет
     */
    public boolean detectChanges(Emr oldEmr, Emr newEmr) {
        return !Objects.equals(oldEmr.getGfr(), newEmr.getGfr()) ||
                !Objects.equals(oldEmr.getPlt(), newEmr.getPlt()) ||
                !Objects.equals(oldEmr.getWbc(), newEmr.getWbc()) ||
                !Objects.equals(oldEmr.getSodium(), newEmr.getSodium()) ||
                !Objects.equals(oldEmr.getSat(), newEmr.getSat()) ||
                !Objects.equals(oldEmr.getWeight(), newEmr.getWeight()) ||
                !Objects.equals(oldEmr.getChildPughScore(), newEmr.getChildPughScore());
    }

    /*
     * Проверить критические изменения и сгенерировать алерты
     * 
     * @param oldEmr Старые данные EMR
     * @param newEmr Новые данные EMR
     * @param mrn MRN пациента
     * @return Список алертов о критических изменениях
     */
    public List<EmrChangeAlertDTO> checkCriticalChanges(Emr oldEmr, Emr newEmr, String mrn) {
        List<EmrChangeAlertDTO> alerts = new ArrayList<>();

        // GFR критическое падение
        if (isGfrCritical(oldEmr.getGfr(), newEmr.getGfr())) {
            alerts.add(createAlert("GFR", oldEmr.getGfr(), newEmr.getGfr(), 
                    EmrChangeAlertDTO.AlertSeverity.CRITICAL, "Kidney function critically decreased"));
        }

        // PLT < 50
        if (newEmr.getPlt() != null && newEmr.getPlt() < 50) {
            alerts.add(createAlert("PLT", String.valueOf(oldEmr.getPlt()), String.valueOf(newEmr.getPlt()),
                    EmrChangeAlertDTO.AlertSeverity.CRITICAL, "Critically low platelets - bleeding risk"));
        }

        // WBC < 2
        if (newEmr.getWbc() != null && newEmr.getWbc() < 2.0) {
            alerts.add(createAlert("WBC", String.valueOf(oldEmr.getWbc()), String.valueOf(newEmr.getWbc()),
                    EmrChangeAlertDTO.AlertSeverity.CRITICAL, "Critically low white blood cells - immunodeficiency"));
        }

        // SAT < 90
        if (newEmr.getSat() != null && newEmr.getSat() < 90) {
            alerts.add(createAlert("SAT", String.valueOf(oldEmr.getSat()), String.valueOf(newEmr.getSat()),
                    EmrChangeAlertDTO.AlertSeverity.CRITICAL, "Critical hypoxia"));
        }

        if (!alerts.isEmpty()) {
            log.warn("Detected {} critical changes for patient {}", alerts.size(), mrn);
            alerts.forEach(a -> log.warn(" {}: {} → {}", a.getParameterName(), a.getOldValue(), a.getNewValue()));
        }
        
        return alerts;
    }

    /**
     * Проверка критического изменения GFR
     */
    private boolean isGfrCritical(String oldVal, String newVal) {
        if (oldVal == null || newVal == null) return false;

        try {
            double oldNum = parseDouble(oldVal);
            double newNum = parseDouble(newVal);
            // Критично если упал более чем на 20 единиц или стал меньше 15
            return oldNum - newNum > 20 || newNum < 15;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Парсинг числа из строки (убирает символы >, <, =)
     */
    private double parseDouble(String value) {
        String cleaned = value.replaceAll("[><= ]", "");
        return Double.parseDouble(cleaned);
    }

    /**
     * Создать алерт
     */
    private EmrChangeAlertDTO createAlert(String param, String oldVal, String newVal, 
                                          EmrChangeAlertDTO.AlertSeverity severity, String msg) {
        return EmrChangeAlertDTO.builder()
                .parameterName(param)
                .oldValue(oldVal)
                .newValue(newVal)
                .severity(severity)
                .changeDescription(msg)
                .detectedAt(LocalDateTime.now())
                .requiresRecommendationReview(true)
                .build();
    }
}