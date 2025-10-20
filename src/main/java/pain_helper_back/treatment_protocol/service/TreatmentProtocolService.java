package pain_helper_back.treatment_protocol.service;

import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pain_helper_back.common.patients.entity.*;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.enums.DrugRoute;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.repository.TreatmentProtocolRepository;

import java.util.ArrayList;
import java.util.List;


/**
 * Главный оркестратор применения протокола лечения (TreatmentProtocolService):
 * 1. Фильтрует протоколы по уровню боли.
 * 2. Для каждого создаёт Recommendation с MAIN и ALTERNATIVE препаратами.
 * 3. Последовательно применяет все TreatmentRuleApplier (9 фильтров).
 * 4. Если хотя бы один препарат остался активным, добавляет рекомендацию в результат.
 * 5. Добавляет противопоказания (contraindications) в комментарии.
 */

@Service
//@RequiredArgsConstructor
@Slf4j
public class TreatmentProtocolService {
    private final TreatmentProtocolRepository treatmentProtocolRepository;
    private final List<TreatmentRuleApplier> ruleAppliers;
    private final ModelMapper modelMapper;


    public TreatmentProtocolService(TreatmentProtocolRepository treatmentProtocolRepository,
                                    List<TreatmentRuleApplier> ruleAppliers, ModelMapper modelMapper) {
        this.treatmentProtocolRepository = treatmentProtocolRepository;
        this.ruleAppliers = ruleAppliers;
        log.info(" Loaded TreatmentRuleAppliers (Классы-фильтры, реализующие интерфейс TreatmentRuleApplier): {}",
                ruleAppliers.stream().map(r -> r.getClass().getSimpleName()).toList());
        this.modelMapper = modelMapper;
    }

    /**
     * Текущая сигнатура возвращает single Recommendation (первую соответствующую).
     * Если нужно вернуть все, меняем сигнатуру на List<Recommendation>.
     */
    public Recommendation generateRecommendation(Vas vas, Patient patient) {
        Integer painLevel = vas.getPainLevel();
        List<TreatmentProtocol> painRageFilter = treatmentProtocolRepository.findAll().stream()
                .filter(tp -> {
                    int[] range = parsePainLevel(tp.getPainLevel());
                    int painLevelFrom = range[0];
                    int painLevelTo = range[1];
                    return painLevel >= painLevelFrom && painLevel <= painLevelTo;
                }).toList();


        List<Recommendation> recommendations = new ArrayList<>();
        Recommendation recommendationFailed = new Recommendation(); // на случай есл все рекомендации отвергнуться
        List<String> rejectionReasons = new ArrayList<>();  // причины отказов этих рекомендаций

        for (TreatmentProtocol tp : painRageFilter) {
            Recommendation recommendation = new Recommendation();
            recommendation.setStatus(RecommendationStatus.PENDING);
            recommendation.setRegimenHierarchy(Integer.parseInt(tp.getRegimenHierarchy()));
            // создаём две записи: основное и запасное (или просто две позиции)
            DrugRecommendation mainDrug = new DrugRecommendation();
            mainDrug.setRole(DrugRole.MAIN);
            DrugRecommendation altDrug = new DrugRecommendation();
            altDrug.setRole(DrugRole.ALTERNATIVE);
            mainDrug.setRecommendation(recommendation);
            altDrug.setRecommendation(recommendation);
            recommendation.getDrugs().add(mainDrug);
            recommendation.getDrugs().add(altDrug);
            // Заполняем общие поля (route, полевые служебные данные) можно здесь или в апликаторах
            mainDrug.setRoute(DrugRoute.valueOf(tp.getRoute()));
            altDrug.setRoute(DrugRoute.valueOf(tp.getRoute()));
            for (TreatmentRuleApplier ruleApplier : ruleAppliers) {
                // Динамика боли (VAS). Анализирует последние жалобы пациента (ухудшения или инверсия).
                // Применяем возрастные правила(<=18 or >75)
                // Contraindications — это список состояний (обычно в виде ICD-10 кодов), участвуют в фильтрации и исключают рекомендацию при наличии заболевания у пациента.
                // Применяем корректировку на чувствительность к препаратам (Sensitivity)
                // Применяем корректировку по тромбоцитам (PLT)
                // Применяем корректировку по лейкоцитам (WBC)
                // Применяем корректировку по сатурации (SAT)

                // Применяем весовые правила (только если вес < 50 — по протоколу)
                // Применяем печёночную корректировку (ChildPugh)
                // Применяем почечную корректировку (GFR)


                // Применяем корректировку по натрию (Sodium)


                ruleApplier.apply(mainDrug, recommendation, tp, patient, rejectionReasons);
                ruleApplier.apply(altDrug, recommendation, tp, patient, rejectionReasons);

            }
            boolean allCleared = recommendation.getDrugs().stream()
                    .allMatch(dr -> dr.getActiveMoiety() == null || dr.getActiveMoiety().isBlank());

            if (allCleared) {
                // все препараты очищены — отклоняем рекомендацию
                recommendationFailed.setGenerationFailed(true);
                recommendationFailed.getRejectionReasonsSummary().addAll(rejectionReasons);
                log.warn(" All drugs cleared for protocol id={}, reasons={}", tp.getId(), rejectionReasons);
            } else {
                // есть хотя бы один живой препарат — сохраняем
                recommendation.setGenerationFailed(false);
                recommendations.add(recommendation);
                log.info(" Recommendation kept: protocol id={} (some drugs active)", tp.getId());
            }

        }
        if (recommendations.isEmpty()) {
            log.warn("No valid recommendations generated for patient {}, all filters rejected drugs", patient.getMrn());
            return recommendationFailed;
        } else {
            log.info("Generated {} valid recommendations for patient {}", recommendations.size(), patient.getMrn());
            // Вернём первую рекомендацию (если их несколько). При желании вернуть все — меняем сигнатуру.
            return recommendations.getFirst();
        }
    }


    private int[] parsePainLevel(String painLevel) {
        if (painLevel == null) return new int[]{0, 0};
        painLevel = painLevel.replaceAll("[^0-9\\-]", "").trim(); // удаляем мусор
        if (painLevel.isEmpty()) return new int[]{0, 0};

        String[] parts = painLevel.split("-");
        try {
            int low = Integer.parseInt(parts[0]);
            int high = (parts.length > 1) ? Integer.parseInt(parts[1]) : low;
            return new int[]{low, high};
        } catch (NumberFormatException e) {
            log.warn(" Invalid pain level '{}'", painLevel);
            return new int[]{0, 0};
        }
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