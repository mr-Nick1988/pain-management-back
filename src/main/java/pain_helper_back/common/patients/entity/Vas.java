package pain_helper_back.common.patients.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/*
 * Entity for хранения VAS (Visual Analog Scale) записей.
 *
 * ПОДДЕРЖКА ВНЕШНЕЙ ИНТЕГРАЦИИ:
 * - vasLevel - level pain (0-10)
 * - recordedAt - время записи
 * - location - локация patient
 * - notes - дополнительные заметки
 * - recordedBy - кто записал (nurse or внешняя система)
 */
@Entity
@Getter
@Setter
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
    // НОВЫЕ ПОЛЯ for ВНЕШНЕЙ ИНТЕГРАЦИИ
    // ============================================

    /**
     * level pain по VAS (0-10)
     * Используется for внешней интеграции
     */
    @Column(name = "vas_level")
    private Integer vasLevel;

    /**
     * Время записи VAS (from внешней системы or текущее)
     */
    @Column(name = "recorded_at")
    private LocalDateTime recordedAt;

    /**
     * Локация patient
     * Exampleы: "Ward A, Bed 12", "ICU-3", "ER-Room-5"
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
     * Exampleы: "nurse_maria", "EXTERNAL_JSON_IMPORT", "EXTERNAL_FHIR_R4_IMPORT"
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

        // Синхронfromация vasLevel и painLevel
        if (this.vasLevel != null && this.painLevel == null) {
            this.painLevel = this.vasLevel;
        } else if (this.painLevel != null && this.vasLevel == null) {
            this.vasLevel = this.painLevel;
        }

        // Установка recordedAt if не заyesно
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