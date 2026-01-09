package pain_helper_back.treatment_protocol.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import pain_helper_back.common.patients.entity.*;
import pain_helper_back.enums.DrugRole;
import pain_helper_back.enums.DrugRoute;
import pain_helper_back.enums.RecommendationStatus;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;
import pain_helper_back.treatment_protocol.repository.TreatmentProtocolRepository;
import pain_helper_back.treatment_protocol.service.exception.StopRecommendationGenerationException;

import java.util.ArrayList;
import java.util.List;


/**
 * Главный оркестратор применения protocolа лечения (TreatmentProtocolService):
 * 1. Фильтрует protocolы по уровню pain.
 * 2. for each созyesёт Recommendation с MAIN и ALTERNATIVE drugами.
 * 3. afterдовательно применяет all TreatmentRuleApplier (9 фильтров).
 * 4. if хотя бы один drug остался активным, добавляет рекоменyesцию в result.
 * 5. Добавляет противопоказания (contraindications) в комментарии.
 */

@Service
@RequiredArgsConstructor
@Slf4j
public class TreatmentProtocolService {
    private final TreatmentProtocolRepository treatmentProtocolRepository;
    private final List<TreatmentRuleApplier> ruleAppliers;
    private final CorrectionAggregator correctionAggregator;
    private final ModelMapper modelMapper;


//    public TreatmentProtocolService(TreatmentProtocolRepository treatmentProtocolRepository,
//                                    List<TreatmentRuleApplier> ruleAppliers, ModelMapper modelMapper) {
//        this.treatmentProtocolRepository = treatmentProtocolRepository;
//        this.ruleAppliers = ruleAppliers;
//        log.info(" Loaded TreatmentRuleAppliers (Classы-фильтры, реалfromующие Interface TreatmentRuleApplier): {}",
//                ruleAppliers.stream().map(r -> r.getClass().getSimpleName()).toList());
//        this.modelMapper = modelMapper;
//    }

    /**
     * Текущая сигнатура Returns single Recommendation (первую соresponseствующую).
     * if need вернуть all, меняем сигнатуру на List<Recommendation>.
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
        Recommendation recommendationFailed = new Recommendation(); // на случай есл all recommendation отвергнуты
        List<String> rejectionReasons = new ArrayList<>();  // причины отказов этих рекоменyesций

        for (TreatmentProtocol tp : painRageFilter) {
            Recommendation recommendation = new Recommendation();
            recommendation.setStatus(RecommendationStatus.PENDING);
            recommendation.setRegimenHierarchy(Integer.parseInt(tp.getRegimenHierarchy()));
            // созyesём две записи: основное и запасное (or просто две позиции)
            DrugRecommendation mainDrug = new DrugRecommendation();
            mainDrug.setRole(DrugRole.MAIN);
            DrugRecommendation altDrug = new DrugRecommendation();
            altDrug.setRole(DrugRole.ALTERNATIVE);
            mainDrug.setRecommendation(recommendation);
            altDrug.setRecommendation(recommendation);
            recommendation.getDrugs().add(mainDrug);
            recommendation.getDrugs().add(altDrug);
            // Заполняем общие поля (route, fieldвые слalreadyбные data) can here or в апликаторах
            mainDrug.setRoute(DrugRoute.valueOf(tp.getRoute()));
            altDrug.setRoute(DrugRoute.valueOf(tp.getRoute()));
            for (TreatmentRuleApplier ruleApplier : ruleAppliers) {
                // Динамика pain (VAS). Аналfromирует afterдние жалобы patient (ухудшения or инверсия).
                // Apply возрастные правила(<=18 or >75)
                // Contraindications — это list состояний (обычно в виде ICD-10 codeов), участвуют в фильтрации и исключают рекоменyesцию при наличии заболевания у patient.
                // Apply корректировку на чувствительность к drugам (Sensitivity)
                // Apply корректировку по тромбоциthere (PLT)
                // Apply корректировку по лейкоциthere (WBC)
                // Apply корректировку по сатурации (SAT)
                // Apply корректировку по натрию (Sodium)
                // Apply печёночную корректировку (ChildPugh)
                // Apply почечную корректировку (GFR)
                // Apply весовые правила (only if вес < 50 — по protocolу)
                try {
                    ruleApplier.apply(mainDrug, recommendation, tp, patient, rejectionReasons);
                    ruleApplier.apply(altDrug, recommendation, tp, patient, rejectionReasons);
                } catch (StopRecommendationGenerationException e) {
                    log.warn("Recommendation generation stopped by {}: {}",
                            ruleApplier.getClass().getSimpleName(), e.getMessage());
                    break;   // прерываем yesльнейшие фильтры
                }

            }
            //  Apply финальные корректировки по doseм и интервалам к каждому drugу, if таких накопилось неhow many
            for (DrugRecommendation drug : recommendation.getDrugs()) {
                correctionAggregator.applyFinalAdjustments(drug);
            }
            // очищаем агрегатор, whatбы не перетянул data на следующего patient
            correctionAggregator.clear();
            boolean allCleared = recommendation.getDrugs().stream()
                    .allMatch(dr ->
                            dr.getActiveMoiety() == null ||
                                    dr.getActiveMoiety().isBlank() ||
                                    dr.getActiveMoiety().equalsIgnoreCase("NA")
                    );

            if (allCleared) {
                // all drugы очищены — отклоняем рекоменyesцию
                recommendationFailed.setGenerationFailed(true);
                recommendationFailed.setStatus(RecommendationStatus.ESCALATED);
                recommendationFailed.getRejectionReasonsSummary().addAll(rejectionReasons);
                log.warn(" All drugs cleared for protocol id={}, reasons={}", tp.getId(), rejectionReasons);
            } else {
                // is хотя бы один живой drug — Save
                recommendation.setGenerationFailed(false);
                recommendations.add(recommendation);
                log.info(" Recommendation kept: protocol id={} (some drugs active)", tp.getId());
            }

        }
        if (recommendations.isEmpty()) {
            log.warn("""
                    [SUMMARY] Patient {} — all recommendations rejected.
                    Reasons: {}
                    """, patient.getMrn(), rejectionReasons);
            // Delete дубликаты, т.к. PainTrendRuleApplier добавляет одну и ту же причину for allх protocolов
            recommendationFailed.setRejectionReasonsSummary(recommendationFailed.getRejectionReasonsSummary().stream().distinct().toList());
            return recommendationFailed;
        } else {
            log.info("Generated {} valid recommendations for patient {}", recommendations.size(), patient.getMrn());
            // Вернём первую рекоменyesцию (if их неhow many). При желании вернуть all — меняем сигнатуру.
            return recommendations.getFirst();
        }
    }


    private int[] parsePainLevel(String painLevel) {
        if (painLevel == null) return new int[]{0, 0};
        painLevel = painLevel.replaceAll("[^0-9\\-]", "").trim(); // Delete мусор
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
//        List<Recommendation> recommendations = painRageFilter.stream()    // созyesли after фильтрации строк таблицы по pain сразу objectы recommendation и yesлее будем инициалfromировать поля
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