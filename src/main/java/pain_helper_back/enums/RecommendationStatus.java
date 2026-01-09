package pain_helper_back.enums;

public enum RecommendationStatus {
    PENDING,       // Создана, ждёт одобрения doctorа
    APPROVED,      // Одобрена (любым doctorом — доктором or anesthesiologistом)
    REJECTED,      // Отклонена (любым doctorом)
    ESCALATED,      // beforeана выше по цепочке (к anesthesiologistу, pain team и т.д.)
    EXECUTED,      // выполнена: medicine выдано/введено
    REQUIRES_REVIEW // TODO - вынести отдельно from бfromнесс логики


}