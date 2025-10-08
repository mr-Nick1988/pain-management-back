package pain_helper_back.enums;

public enum RecommendationStatus {
    PENDING,                          // Ожидает одобрения врача
    APPROVED_BY_DOCTOR,              // Одобрено врачом
    REJECTED_BY_DOCTOR,              // Отклонено врачом
    ESCALATED_TO_ANESTHESIOLOGIST,   // Эскалировано анестезиологу
    APPROVED_BY_ANESTHESIOLOGIST,    // Одобрено анестезиологом
    REJECTED_BY_ANESTHESIOLOGIST,    // Отклонено анестезиологом
    FINAL_APPROVED,                  // Финальное одобрение
    CANCELLED                        // Отменено
}
