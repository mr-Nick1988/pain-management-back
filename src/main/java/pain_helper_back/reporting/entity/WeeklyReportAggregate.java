package pain_helper_back.reporting.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;


/*
 * Агрегированный еженедельный отчет
 * Создается каждый понедельник за прошлую неделю
 *
 * ФОКУС: Эскалации и их разрешение
 */
@Entity
@Table(name = "weekly_report_aggregates")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class WeeklyReportAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Начало недели (понедельник)
     */
    @Column(nullable = false)
    private LocalDate weekStartDate;

    /**
     * Конец недели (воскресенье)
     */
    @Column(nullable = false)
    private LocalDate weekEndDate;

    // ============================================
    // СТАТИСТИКА ЭСКАЛАЦИЙ
    // ============================================

    private Long totalEscalations;
    private Long resolvedEscalations;
    private Long pendingEscalations;
    private Long cancelledEscalations;

    /**
     * Среднее время разрешения (часы)
     */
    private Double averageResolutionTimeHours;

    /**
     * Медианное время разрешения (часы)
     */
    private Double medianResolutionTimeHours;

    /**
     * Максимальное время разрешения (часы)
     */
    private Long maxResolutionTimeHours;

    /**
     * Минимальное время разрешения (часы)
     */
    private Long minResolutionTimeHours;

    // ============================================
    // ПРИЧИНЫ ЭСКАЛАЦИЙ (JSON)
    // ============================================

    /**
     * Распределение по причинам эскалаций (JSON)
     * Формат: {"Pain increase": 23, "Side effects": 12, "Ineffective": 8, ...}
     */
    @Column(columnDefinition = "TEXT")
    private String escalationReasonsJson;

    /**
     * Распределение по приоритетам (JSON)
     * Формат: {"HIGH": 23, "MEDIUM": 45, "LOW": 12}
     */
    @Column(columnDefinition = "TEXT")
    private String escalationsByPriorityJson;

    /**
     * Распределение по статусам (JSON)
     */
    @Column(columnDefinition = "TEXT")
    private String escalationsByStatusJson;

    // ============================================
    // ПРОИЗВОДИТЕЛЬНОСТЬ АНЕСТЕЗИОЛОГОВ
    // ============================================

    /**
     * Количество анестезиологов, работавших на неделе
     */
    private Long activeAnesthesiologists;

    /**
     * Среднее количество эскалаций на анестезиолога
     */
    private Double averageEscalationsPerAnesthesiologist;

    // ============================================
    // МЕТАДАННЫЕ
    // ============================================

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private String createdBy;

    private Long sourceEventsCount;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.createdBy == null) {
            this.createdBy = "system";
        }
    }
}
