package pain_helper_back.treatment_protocol.utils;

import pain_helper_back.common.patients.entity.DrugRecommendation;
import pain_helper_back.common.patients.entity.Recommendation;

import java.util.Optional;

public class SafeValueUtils {

    //  Безопасно Extract имена drugов (избегаем NPE, если list пуст)
    //  Эти строки нужны, чтобы корректно вывести имена drugов в логах и причинах отказа.
    // Если какой-то drug отсутствует, подставится "N/A", чтобы избежать NullPointerException.

    public static String safeValue(DrugRecommendation drugRecommendation) {
        if (drugRecommendation == null) return "N/A";
        return Optional.ofNullable(drugRecommendation.getDrugName())
                .or(() -> Optional.ofNullable(drugRecommendation.getActiveMoiety()))
                .orElse("N/A");



    }
}
