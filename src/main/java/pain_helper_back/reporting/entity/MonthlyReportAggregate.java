package pain_helper_back.reporting.entity;


import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/*
 * Агрегированный ежемесячный отчет
 * Создается 1-го числа каждого месяца за прошлый месяц
 *
 * ФОКУС: Эффективность лечения (VAS до/после)
 */
@Entity
@Table(name = "monthly_report_aggregates")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MonthlyReportAggregate {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Год и месяц отчета (например, 2025-10)
     */
    @Column(nullable = false, unique = true)
    private String reportMonth; // Формат: "2025-10"

    // ============================================
    // ЭФФЕКТИВНОСТЬ ЛЕЧЕНИЯ (VAS)
    // ============================================

    /**
     * Средний VAS до лечения
     */
    private Double averageVasBefore;

    /**
     * Средний VAS после лечения
     */
    private Double averageVasAfter;

    /**
     * Среднее снижение боли (в баллах)
     */
    private Double averagePainReduction;

    /**
     * Процент успешных случаев (снижение VAS >= 2 балла)
     */
    private Double successRate;

    /**
     * Количество пациентов с полным купированием боли (VAS = 0)
     */
    private Long patientsWithZeroPain;

    // ============================================
    // СТАТИСТИКА ПО ПРЕПАРАТАМ (JSON)
    // ============================================

    /**
     * Эффективность по препаратам (JSON)
     * Формат: {"Morphine": 2.5, "Fentanyl": 3.1, "Tramadol": 1.8, ...}
     * Значение = среднее снижение VAS для этого препарата
     */
    @Column(columnDefinition = "TEXT")
    private String painReductionByDrugJson;

    /**
     * Частота назначения препаратов (JSON)
     * Формат: {"Morphine": 145, "Fentanyl": 98, ...}
     */
    @Column(columnDefinition = "TEXT")
    private String drugUsageFrequencyJson;

    // ============================================
    // ЭФФЕКТИВНОСТЬ ПО КАТЕГОРИЯМ ПАЦИЕНТОВ
    // ============================================

    /**
     * Эффективность по возрастным группам (JSON)
     * Формат: {"18-29": 2.8, "30-44": 2.5, "45-59": 2.2, "60-74": 1.9, "75+": 1.5}
     */
    @Column(columnDefinition = "TEXT")
    private String effectivenessByAgeGroupJson;

    /**
     * Эффективность по полу (JSON)
     * Формат: {"MALE": 2.4, "FEMALE": 2.6}
     */
    @Column(columnDefinition = "TEXT")
    private String effectivenessByGenderJson;

    // ============================================
    // ОБЩАЯ СТАТИСТИКА
    // ============================================

    /**
     * Общее количество пациентов за месяц
     */
    private Long totalPatients;

    /**
     * Общее количество рекомендаций
     */
    private Long totalRecommendations;

    /**
     * Общее количество VAS записей
     */
    private Long totalVasRecords;

    /**
     * Процент одобрения рекомендаций
     */
    private Double approvalRate;

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
