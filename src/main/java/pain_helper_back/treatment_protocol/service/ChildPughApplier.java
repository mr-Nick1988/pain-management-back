package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.utils.DrugUtils;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(8)
@Slf4j
public class ChildPughApplier implements TreatmentRuleApplier {

    // Печеночная недостаточность может быть A|B|C категории
    private static final Pattern CHILD_PUGH_PATTERN = Pattern.compile("([ABC])\\s*-\\s*([^A|B|C]+)(?=$|[ABC])");

    @Override
    public void apply(DrugRecommendation drug, Recommendation recommendation, TreatmentProtocol tp, Patient patient) {

        // Если препарат ранее был отвергнут по возрасту (или не заполнен) — не применяем печёночную корректировку
        if (!DrugUtils.hasInfo(drug)) return;

        String patientChildPugh = patient.getEmr().getLast().getChildPughScore(); // "A", "B" или "C"
        String childPughRule = (drug.getRole() == DrugRole.MAIN)
                ? tp.getFirstChildPugh()
                : tp.getSecondChildPugh();

        // 1️⃣ Проверяем, есть ли вообще данные в ячейке
        if (childPughRule == null || childPughRule.isBlank() || childPughRule.equalsIgnoreCase("NA")) {
            log.debug("ℹ️ ChildPugh rule empty or NA for protocol {}", tp.getId());
            return;
        }

        // 2️⃣ Нормализуем символы
        childPughRule = childPughRule
                .replace("–", "-")
                .replace("—", "-")
                .replace("\u00A0", " ")
                .trim();

        // 3️⃣ Извлекаем шаблон “A - 8h B - 12h C - avoid”
        Matcher m = CHILD_PUGH_PATTERN.matcher(childPughRule);
        Map<String, String> liverRules = new HashMap<>();
        while (m.find()) {
            liverRules.put(m.group(1), m.group(2).trim()); // {"A" -> "50 mg 8h", "B" -> "25 mg 12h", "C" -> "avoid"}
        }

        // 4️⃣ Ищем конкретное правило для категории пациента
        String patientRule = liverRules.get(patientChildPugh);

        if (patientRule == null || patientRule.isBlank()) {
            log.debug("⚠️ No ChildPugh rule found for category {} in protocol {}", patientChildPugh, tp.getId());
            return;
        }

        patientRule = patientRule.toLowerCase();
        //если содержит "avoid" → обнулить объект DrugRecommendation и добавить комментарий в recommendation.getComments()
        //если содержит "mg" → изменить drug.setDosing()
        //если содержит "h" → изменить drug.setInterval()

        // 5️⃣ Применяем правило
        if (patientRule.contains("avoid")) {
            recommendation.getComments().add("System: avoid for Child-Pugh " + patientChildPugh);
            DrugUtils.clearDrug(drug);
            return;
        }

        Matcher mg = Pattern.compile("(\\d+)\\s*mg").matcher(patientRule);
        if (mg.find()) {
            drug.setDosing(mg.group(1) + " mg");
        }

        Matcher h = Pattern.compile("(\\d+)\\s*h").matcher(patientRule);
        if (h.find()) {
            drug.setInterval(h.group(1) + "h");
        }

        log.debug("✅ Applied ChildPugh rule '{}' for {} category (protocol {})",
                patientRule, patientChildPugh, tp.getId());
    }
}
