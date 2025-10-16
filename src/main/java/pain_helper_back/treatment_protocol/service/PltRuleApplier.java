package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(4)
@Slf4j
public class PltRuleApplier implements TreatmentRuleApplier{

/*
PLT (platelet count) — это количество тромбоцитов (platelets) в крови.
Измеряется в тысячах на микролитр крови:
👉 1K/µL = 1000 тромбоцитов на микролитр.
150K–450K/µL  →  норма
<100K/µL      →  риск кровотечения, нужно избегать определённых препаратов
*/

    // Пример формата: "<100K/µL - avoid"
    private static final Pattern PLT_PATTERN = Pattern.compile("([<>]=?)\\s*(\\d+)\\s*[Kk]?/?µ?[lL]");

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {
        // если препарат пустой или уже "отвергнут" ранее — не трогаем
        if (!DrugUtils.hasInfo(drug)) return;

        Double patientPlt = patient.getEmr().getLast().getPlt(); // например, "92" или "120K/µL"
        String rule = tp.getPlt();                                 // например, "<100K/µL - avoid"

        if (rule == null || rule.trim().isEmpty() || rule.equalsIgnoreCase("NA")) return;
        if (patientPlt == null) {
            throw new IllegalArgumentException("Patient PLT is null");
        }


        Matcher m = PLT_PATTERN.matcher(rule);
        if (!m.find()) return;

        String operator = m.group(1); // "<" или ">"
        double limit = Double.parseDouble(m.group(2));

        // Проверяем условие
        boolean below = operator.contains("<") && patientPlt < limit;
        boolean above = operator.contains(">") && patientPlt > limit;

        //Если написано “avoid” — добавляем комментарий и обнуляем объекты DrugRecommendation и останавливаемся.
        if ((below || above) && rule.toLowerCase().contains("avoid")) {
            for (DrugRecommendation drugs: recommendation.getDrugs()){
                DrugUtils.clearDrug(drugs);
            }
            recommendation.getComments().add("System: avoid for PLT < " + limit + "K/µL (" + patientPlt + "K/µL)");
            log.info("Avoid triggered by PLT rule: patient={}, value={}, rule={}", patient.getId(), patientPlt, rule);

        }
    }
}




