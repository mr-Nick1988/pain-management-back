package pain_helper_back.enums;

public enum RecommendationStatus {
    PENDING,       // Создана, ждёт одобрения врача
    APPROVED,      // Одобрена (любым врачом — доктором or анестезиологом)
    REJECTED,      // Отклонена (любым врачом)
    ESCALATED,      // beforeана выше по цепочке (к анестезиологу, pain team и т.д.)
    EXECUTED,      // выполнена: лекарство выдано/введено
    REQUIRES_REVIEW // TODO - вынести отдельно from бfromнесс логики


}