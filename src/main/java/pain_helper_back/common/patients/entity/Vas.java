package pain_helper_back.common.patients.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

/*
 * Entity для хранения VAS (Visual Analog Scale) записей.
 *
 * ПОДДЕРЖКА ВНЕШНЕЙ ИНТЕГРАЦИИ:
 * - vasLevel - уровень боли (0-10)
 * - recordedAt - время записи
 * - location - локация пациента
 * - notes - дополнительные заметки
 * - recordedBy - кто записал (медсестра или внешняя система)
 */
@Entity
@Data
@Table(name = "vas")
public class Vas {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vas_id")
    private Long id;

    @Column(name = "pain_place")
    private String painPlace;

    @Column(name = "pain_level")
    private Integer painLevel; // 0-10 scale
    @Column(name = "is_resolved")
    private boolean resolved = false;



    // ============================================
    // НОВЫЕ ПОЛЯ ДЛЯ ВНЕШНЕЙ ИНТЕГРАЦИИ
    // ============================================

    /**
     * Уровень боли по VAS (0-10)
     * Используется для внешней интеграции
     */
    @Column(name = "vas_level")
    private Integer vasLevel;

    /**
     * Время записи VAS (из внешней системы или текущее)
     */
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    /**
     * Локация пациента
     * Примеры: "Ward A, Bed 12", "ICU-3", "ER-Room-5"
     */
    @Column(name = "location", length = 255)
    private String location;

    /**
     * Дополнительные заметки
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Кто записал VAS
     * Примеры: "nurse_maria", "EXTERNAL_JSON_IMPORT", "EXTERNAL_FHIR_R4_IMPORT"
     */
    @Column(name = "recorded_by", length = 100)
    private String recordedBy;

    // ============================================
    // AUDIT ПОЛЯ
    // ============================================


    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", updatable = false)
    private String createdBy;

    @Column(name = "updated_by")
    private String updatedBy;

    @ManyToOne
    @JoinColumn(name = "patient_id")
    private Patient patient;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();

        if (this.createdBy == null) {
            this.createdBy = "system";
        }

        // Синхронизация vasLevel и painLevel
        if (this.vasLevel != null && this.painLevel == null) {
            this.painLevel = this.vasLevel;
        } else if (this.painLevel != null && this.vasLevel == null) {
            this.vasLevel = this.painLevel;
        }

        // Установка recordedAt если не задано
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();

        if (this.updatedBy == null) {
            this.updatedBy = "system";
        }
    }
}