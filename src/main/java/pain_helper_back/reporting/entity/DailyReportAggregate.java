package pain_helper_back.reporting.entity;


/*
 * Агрегированный ежедневный отчет
 * Хранится в PostgreSQL (или H2 для разработки)
 * Создается автоматически каждую ночь из данных MongoDB
 *
 * НАЗНАЧЕНИЕ:
 * - Долгосрочное хранение агрегированных данных
 * - Быстрый доступ к историческим метрикам
 * - Основа для генерации отчетов в PDF/Excel
 *
 * ИСТОЧНИК ДАННЫХ: MongoDB (AnalyticsEvent)
 * СОЗДАЕТСЯ: DataAggregationService (@Scheduled каждую ночь в 00:30)
 */

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_report_aggregates")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DailyReportAggregate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * Дата отчета (за какой день агрегированы данные)
     */
    @Column(nullable = false, unique = true)
    private LocalDate reportDate;

    // ============================================
    // СТАТИСТИКА ПАЦИЕНТОВ
    // ============================================

    /*
     * Количество зарегистрированных пациентов за день
     */
    private Long totalPatientsRegistered;
    /**
     * Количество записей VAS (измерений боли) за день
     */
    private Long totalVasRecords;


    /**
     * Средний уровень боли по VAS (0-10)
     */
    private Double averageVasLevel;

    /**
     * Количество критических случаев (VAS >= 7)
     */
    private Long criticalVasCount;

    // ============================================
    // СТАТИСТИКА РЕКОМЕНДАЦИЙ
    // ============================================

    /**
     * Общее количество сгенерированных рекомендаций
     */
    private Long totalRecommendations;

    /**
     * Количество одобренных рекомендаций
     */
    private Long approvedRecommendations;

    /**
     * Количество отклоненных рекомендаций
     */
    private Long rejectedRecommendations;

    /**
     * Процент одобрения (approvedRecommendations / totalRecommendations * 100)
     */
    private Double approvalRate;

    // ============================================
    // СТАТИСТИКА ЭСКАЛАЦИЙ
    // ============================================

    /**
     * Общее количество созданных эскалаций
     */
    private Long totalEscalations;

    /**
     * Количество разрешенных эскалаций
     */
    private Long resolvedEscalations;

    /**
     * Количество ожидающих эскалаций
     */
    private Long pendingEscalations;

    /**
     * Среднее время разрешения эскалации (в часах)
     */
    private Double averageResolutionTimeHours;

    // ============================================
    // ПРОИЗВОДИТЕЛЬНОСТЬ СИСТЕМЫ
    // ============================================

    /**
     * Среднее время обработки операций (в миллисекундах)
     */
    private Double averageProcessingTimeMs;

    /**
     * Общее количество операций
     */
    private Long totalOperations;

    /**
     * Количество неудачных операций
     */
    private Long failedOperations;

    // ============================================
    // АКТИВНОСТЬ ПОЛЬЗОВАТЕЛЕЙ
    // ============================================

    /**
     * Общее количество входов в систему
     */
    private Long totalLogins;

    /**
     * Количество уникальных активных пользователей
     */
    private Long uniqueActiveUsers;

    /**
     * Количество неудачных попыток входа
     */
    private Long failedLoginAttempts;

    // ============================================
    // ИСПОЛЬЗОВАНИЕ ПРЕПАРАТОВ (JSON)
    // ============================================

    /*
     * Топ-10 наиболее назначаемых препаратов (JSON)
     * Формат: {"Morphine": 45, "Fentanyl": 32, "Tramadol": 28, ...}
     *
     * ЗАЧЕМ JSON:
     * - Гибкость (количество препаратов может меняться)
     * - Не нужно создавать отдельную таблицу
     * - Легко парсится для отчетов
     */
    @Column(columnDefinition = "TEXT")
    private String topDrugsJson;

    /**
     * Статистика корректировок доз (JSON)
     * Формат: {"Age": 12, "Liver": 8, "Kidney": 15, "Weight": 6, ...}
     */
    @Column(columnDefinition = "TEXT")
    private String doseAdjustmentsJson;

    // ============================================
    // МЕТАДАННЫЕ
    // ============================================

    /**
     * Дата и время создания агрегата
     */
    @Column(nullable = false)
    private LocalDateTime createdAt;

    /**
     * Кто создал агрегат (обычно "system")
     */
    @Column(nullable = false)
    private String createdBy;

    /**
     * Количество событий, обработанных для создания агрегата
     * (для контроля качества данных)
     */
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
