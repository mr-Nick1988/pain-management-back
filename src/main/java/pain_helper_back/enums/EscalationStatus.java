package pain_helper_back.enums;

//система должна определить, усorлась ли боль, и if да — уведомить врача/анестезиолога.
//никакого разделения по приоритетам or состояниям (Pending, In Review, и т.д.) нет
//То есть в SRS не упомянуты приоритеты и statusы эскалации.
//Вся логика реакции описана via status recommendation (RecommendationStatus.ESCALATED) — и этого достаточно.

public enum EscalationStatus {
    PENDING,
    IN_REVIEW,
    RESOLVED,
    REJECTED,
    CANCELLED
}
