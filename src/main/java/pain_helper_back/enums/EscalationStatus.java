package pain_helper_back.enums;

//система должна определить, усилилась ли боль, и если да — уведомить врача/анестезиолога.
//никакого разделения по приоритетам или состояниям (Pending, In Review, и т.д.) нет
//То есть в SRS не упомянуты приоритеты и статусы эскалации.
//Вся логика реакции описана через статус рекомендации (RecommendationStatus.ESCALATED) — и этого достаточно.

public enum EscalationStatus {
    PENDING,
    IN_REVIEW,
    RESOLVED,
    REJECTED,
    CANCELLED
}
