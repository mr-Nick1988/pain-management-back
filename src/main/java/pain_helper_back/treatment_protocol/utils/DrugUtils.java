package pain_helper_back.treatment_protocol.utils;

import pain_helper_back.common.patients.entity.DrugRecommendation;

public class DrugUtils {
    public static void clearDrug(DrugRecommendation drug) {
        drug.setDrugName(null);
        drug.setActiveMoiety(null);
        drug.setDosing(null);
        drug.setInterval(null);
        drug.setAgeAdjustment(null);
        drug.setWeightAdjustment(null);
        drug.setChildPugh(null);
        drug.setRoute(null);
    }

    public static boolean hasInfo(DrugRecommendation d) {
        return d != null && d.getActiveMoiety() != null && !d.getActiveMoiety().isBlank();
    }
}
