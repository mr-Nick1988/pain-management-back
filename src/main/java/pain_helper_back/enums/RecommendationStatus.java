package pain_helper_back.enums;

public enum RecommendationStatus {
    PENDING,       // Создана, ждёт одобрения врача
    APPROVED,      // Одобрена (любым врачом — доктором или анестезиологом)
    REJECTED,      // Отклонена (любым врачом)
    ESCALATED,      // Передана выше по цепочке (к анестезиологу, pain team и т.д.)
     EXECUTED,      // выполнена: лекарство выдано/введено
    REQUIRES_REVIEW // TODO - вынести отдельно из бизнесс логики
}