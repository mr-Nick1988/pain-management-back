package pain_helper_back.treatment_protocol.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.nurse.entity.*;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.repository.TreatmentProtocolRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class TreatmentProtocolService {
    private final TreatmentProtocolRepository treatmentProtocolRepository;

    // Паттерн для поиска последнего числа в строке (поддержка десятичных)
    private static final Pattern LAST_NUMBER_PATTERN = Pattern.compile(("(\\d+)(?=[^0-9]*$)"));
    private static final Pattern FIRST_INT_PATTERN = Pattern.compile("(\\d+)");

    /**
     * Текущая сигнатура возвращает single Recommendation (первую соответствующую).
     * Если нужно вернуть все, поменяй сигнатуру на List<Recommendation>.
     */
    public Recommendation generateRecommendation(Emr emr, Vas vas, Patient patient) {
        Integer painLevel = vas.getPainLevel();
        List<TreatmentProtocol> painRageFilter = treatmentProtocolRepository.findAll().stream()
                .filter(tp -> {
                    String[] range = tp.getPainLevel().split("-");
                    int painLevelFrom = Integer.parseInt(range[0].trim());
                    int painLevelTo = Integer.parseInt(range[1].trim());
                    return painLevel >= painLevelFrom && painLevel <= painLevelTo;
                }).toList();

        Integer patientAge = patient.getAge();
        Double patientWeight = patient.getWeight();

        List<Recommendation> recommendations = new ArrayList<>();

        for (TreatmentProtocol tp : painRageFilter) {
            Recommendation recommendation = new Recommendation();
            recommendation.setStatus(RecommendationStatus.PENDING);
            recommendation.setRegimenHierarchy(Integer.parseInt(tp.getRegimenHierarchy()));
            // создаём две записи: основное и запасное (или просто две позиции)
            DrugRecommendation mainDrug = new DrugRecommendation();
            DrugRecommendation altDrug = new DrugRecommendation();

            recommendation.getDrugs().add(mainDrug);
            recommendation.getAlternativeDrugs().add(altDrug);

            // Заполняем общие поля (route, полевые служебные данные) можно здесь или в апликаторах
            mainDrug.setRoute(tp.getRoute());
            altDrug.setRoute(tp.getRoute());

            // Применяем возрастные правила
            applyAgeAdjustment(mainDrug, recommendation, tp, 1, patientAge);
            applyAgeAdjustment(altDrug, recommendation, tp, 2, patientAge);

            // Применяем весовые правила (только если вес < 50 — по протоколу)
            applyWeightAdjustment(mainDrug, recommendation, tp, 1, patientWeight);
            applyWeightAdjustment(altDrug, recommendation, tp, 2, patientWeight);

            // Сохраняем служебную информацию из таблицы
            if (tp.getAvoidIfSensitivity() != null && !tp.getAvoidIfSensitivity().isBlank()) {
                recommendation.getAvoidIfSensitivity().add(tp.getAvoidIfSensitivity());
            }
            if (tp.getContraindications() != null && !tp.getContraindications().isBlank()) {
                recommendation.getContraindications().add(tp.getContraindications());
            }

            recommendations.add(recommendation);
        }

        // Вернём первую рекомендацию (если их несколько). При желании вернуть все — меняем сигнатуру.
        return recommendations.isEmpty() ? null : recommendations.get(0);
    }

    /**
     * Применяет возрастное правило к конкретной прописке препарата.
     * Если препарат разрешён — заполняет данные из TP. Иначе добавляет заметку в recommendation.notes.
     */
    private void applyAgeAdjustment(DrugRecommendation drug,
                                    Recommendation recommendation,
                                    TreatmentProtocol tp,
                                    int drugIndex,
                                    Integer patientAge) {
        String ageAdjustment = (drugIndex == 1) ? tp.getFirstAgeAdjustments() : tp.getSecondAgeAdjustments();

        // Если правило пустое или "NA" — возраст не ограничивает
        if (ageAdjustment == null || ageAdjustment.trim().isEmpty() || ageAdjustment.toUpperCase().contains("NA")) {
            // разрешаем препарат — заполняем поля
            fillDrugFromProtocol(drug, tp, drugIndex);
            return;
        }

        Integer limit = extractFirstInt(ageAdjustment); // берём первое число (например 75 или 18)
        if (limit == null) {
            // не смогли распарсить число — безопасно разрешаем и записываем в notes, чтобы посмотреть
            fillDrugFromProtocol(drug, tp, drugIndex);
            recommendation.getNotes().add("Warning: unable to parse age rule '" + ageAdjustment + "'");
            return;
        }

        // Логика  протокола:
        // - Для первого препарата: если patientAge > limit => avoid
        // - Для второго препарата: если patientAge < limit => avoid
        if (drugIndex == 1) {
            if (patientAge <= limit) {
                fillDrugFromProtocol(drug, tp, drugIndex);
            } else {
                recommendation.getNotes().add("First drug avoid: patient age (" + patientAge + ") > " + limit);
            }
        } else { // drugIndex == 2
            if (patientAge >= limit) {
                fillDrugFromProtocol(drug, tp, drugIndex);
            } else {
                recommendation.getNotes().add("Second drug avoid: patient age (" + patientAge + ") < " + limit);
            }
        }
    }

    /**
     * Заполняет поля DrugRecommendation из строки протокола (по индексу лекарства).
     * Не выполняет проверок — просто копирует данные.
     */
    private void fillDrugFromProtocol(DrugRecommendation drug, TreatmentProtocol tp, int drugIndex) {
        if (drugIndex == 1) {
            drug.setDrugName(tp.getFirstDrug());
            drug.setActiveMoiety(tp.getFirstDrugActiveMoiety());
            drug.setDosing(tp.getFirstDosingMg());
            drug.setInterval(tp.getFirstIntervalHrs());
            drug.setAgeAdjustment(tp.getFirstAgeAdjustments());
            drug.setWeightAdjustment(tp.getWeightKg());
            drug.setChildPugh(tp.getFirstChildPugh());
            // route  установили ранее
        } else {
            // второе "вещество" — имя препарата может отсутствовать, заполняем доступное
            drug.setDrugName(null); // если у тебя нет имени для second drug
            drug.setActiveMoiety(tp.getSecondDrugActiveMoiety());
            drug.setDosing(tp.getSecondDosingMg());
            drug.setInterval(tp.getSecondIntervalHrs());
            drug.setAgeAdjustment(tp.getSecondAgeAdjustments());
            drug.setWeightAdjustment(tp.getSecondWeightKg());
            drug.setChildPugh(tp.getSecondChildPugh());
        }
    }

    /**
     * Применяет правило по весу. По протоколу корректировка применяется только для веса < 50kg.
     * Правило ожидается в формате вроде "<50kg - 8h" или "<50kg - 50mg".
     * Извлекает последнее число в строке и unit (mg/h) и применяет корректировку dose/interval.
     */
    private void applyWeightAdjustment(DrugRecommendation drug,
                                       Recommendation recommendation,
                                       TreatmentProtocol tp,
                                       int drugIndex,
                                       Double patientWeight) {
        // Если веса нет или пациент >= 50kg — по протоколу ничего не делаем
        if (patientWeight == null || patientWeight >= 50.0) return;

        String weightRule = (drugIndex == 1) ? tp.getWeightKg() : tp.getSecondWeightKg();
        if (weightRule == null || weightRule.trim().isEmpty() || weightRule.toUpperCase().contains("NA")) return;

        // Если препарат ранее был отвергнут по возрасту (или не заполнен) — не применяем весовую корректировку
        boolean drugHasInfo =  (drug.getActiveMoiety() != null && !drug.getActiveMoiety().isBlank());
        if (!drugHasInfo) return;

        // ищем последнее число
        Matcher m = LAST_NUMBER_PATTERN.matcher(weightRule);
        if (!m.find()) return;
        String lastNumber = m.group(1);

        // определяем unit: берем хвост строки после найденного числа и оставляем только буквенные символы
        int afterIndex = m.end();
        String suffix = weightRule.substring(afterIndex).replaceAll("[^a-zA-Z%]", "").toLowerCase();

        if (suffix.contains("mg")) {
            // корректировка дозы
            String newDose = lastNumber + " mg";
            drug.setDosing(newDose);
            recommendation.getNotes().add("Dose adjusted for weight <50kg: set dosing to " + newDose
                    + " for " + (drug.getDrugName() != null ? drug.getDrugName() : drug.getActiveMoiety()));
        } else if (suffix.contains("h")) {
            // корректировка интервала
            String newInterval = lastNumber + "h";
            drug.setInterval(newInterval);
            recommendation.getNotes().add("Interval adjusted for weight <50kg: set interval to " + newInterval
                    + " for " + (drug.getDrugName() != null ? drug.getDrugName() : drug.getActiveMoiety()));
        } else {
            // неизвестный суффикс — просто логируем заметку, не ломая данные
            recommendation.getNotes().add("Weight rule parsed but unknown unit '" + suffix + "' in '" + weightRule + "'");
        }
    }

    // Вспомогательная: извлекает первое целое число из строки (например из ">75 years - avoid" даст 75)
    private Integer extractFirstInt(String s) {
        if (s == null) return null;
        Matcher m = FIRST_INT_PATTERN.matcher(s);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {
            }
        }
        return null;
    }
}






//*@Service
//@RequiredArgsConstructor
//public class TreatmentProtocolService {
//    private final TreatmentProtocolRepository treatmentProtocolRepository;
//
//
//    public Recommendation generateRecommendation(Emr emr, Vas vas, Patient patient) {
//        Integer painLevel = vas.getPainLevel();
//        List<TreatmentProtocol> painRageFilter = treatmentProtocolRepository.findAll().stream()
//                .filter(tp -> {
//                    String[] range = tp.getPainLevel().split("-");
//                    int painLevelFrom = Integer.parseInt(range[0]);
//                    int painLevelTo = Integer.parseInt(range[1]);
//                    return painLevel >= painLevelFrom && painLevel <= painLevelTo;
//                }).toList();
//        Integer patientAge = patient.getAge();
//        Double patientWeight = patient.getWeight();
//        List<Recommendation> recommendations = painRageFilter.stream()    // создали после фильтрации строк таблицы по боли сразу объекты рекомендации и далее будем инициализировать поля
//                .map(tp -> {
//                    Recommendation recommendation = new Recommendation();
//                    recommendation.setStatus("PENDING");
//                    recommendation.setRegimenHierarchy(Integer.parseInt(tp.getRegimenHierarchy()));
//                    ageAdjustmentForDrug(recommendation, patientAge, 1, tp);
//                    ageAdjustmentForDrug(recommendation, patientAge, 2, tp);
//                    weightAdjustmentForDrug(recommendation, patientWeight,1, tp);
//                    weightAdjustmentForDrug(recommendation, patientWeight,2, tp);
//                    return recommendation;
//                }).toList();
//
//        return null;
//    }
//
//
//
//    private void ageAdjustmentForDrug(Recommendation recommendation, Integer patientAge, int drugIndex, TreatmentProtocol tp) {
//        DrugRecommendation drugRecommendation = new DrugRecommendation();
//        DrugRecommendation alternativeDrugRecommendation = new DrugRecommendation();
//        recommendation.getDrugs().add(drugRecommendation);
//        recommendation.getAlternativeDrugs().add(alternativeDrugRecommendation);
//        String ageAdjustment;
//        if (drugIndex == 1) ageAdjustment = tp.getFirstAgeAdjustments();
//        else ageAdjustment = tp.getSecondAgeAdjustments();
//        if (ageAdjustment != null && !ageAdjustment.trim().isEmpty() && !ageAdjustment.contains("NA")) {
//            int limit = Integer.parseInt(ageAdjustment.replaceAll("\\D+", ""));
//            if (drugIndex == 1 && limit > patientAge) {
//                drugRecommendation.setDrugName(tp.getFirstDrug());
//                drugRecommendation.setActiveMoiety(tp.getFirstDrugActiveMoiety());
//                drugRecommendation.setDosing(tp.getFirstDosingMg());
//                drugRecommendation.setInterval(tp.getFirstIntervalHrs());
//                drugRecommendation.setRoute(tp.getRoute());
//                drugRecommendation.setAgeAdjustment(tp.getFirstAgeAdjustments());
//                drugRecommendation.setWeightAdjustment(tp.getWeightKg());
//                drugRecommendation.setChildPugh(tp.getFirstChildPugh());
//            } else {
//                recommendation.getNotes().add("First drug avoid: age > " + limit);
//            }
//            if (drugIndex == 2 && limit <= patientAge) {
//                alternativeDrugRecommendation.setActiveMoiety(tp.getSecondDrugActiveMoiety());
//                alternativeDrugRecommendation.setDosing(tp.getSecondDosingMg());
//                alternativeDrugRecommendation.setInterval(tp.getSecondIntervalHrs());
//                alternativeDrugRecommendation.setAgeAdjustment(tp.getSecondAgeAdjustments());
//                alternativeDrugRecommendation.setWeightAdjustment(tp.getSecondWeightKg());
//                alternativeDrugRecommendation.setChildPugh(tp.getSecondChildPugh());
//            } else {
//                recommendation.getNotes().add("Second drug avoid: age < " + limit);
//            }
//        }
//    }
//
//    private void weightAdjustmentForDrug(Recommendation recommendation, Double patientWeight, int drugIndex, TreatmentProtocol tp) {
//        if (patientWeight >= 50) return;
//        DrugRecommendation drugRecommendation = recommendation.getDrugs().getFirst();
//        DrugRecommendation alternativeDrugRecommendation = recommendation.getAlternativeDrugs().getLast();
//        String weightAdjustment;
//        if (drugIndex == 1) weightAdjustment = tp.getWeightKg();
//        else weightAdjustment = tp.getSecondWeightKg();
//        if (weightAdjustment != null && !weightAdjustment.trim().isEmpty() && !weightAdjustment.contains("NA")){
//            if(weightAdjustment.endsWith("h")){
//                Pattern p = Pattern.compile("(\\d+)(?=[^0-9]*$)");
//                Matcher m = p.matcher(weightAdjustment);
//                String lastNumber = m.group(1);
//            }
//        }
//    }
//}*/