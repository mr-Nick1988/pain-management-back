package pain_helper_back.anesthesiologist.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import pain_helper_back.enums.ProtocolStatus;

import java.time.LocalDateTime;

@Entity
@Table(name = "protocols")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreatmentProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long escalationId;
    @Column(nullable = false,length = 500)
    private String title;
    @Column(nullable = false,columnDefinition = "TEXT")
    private String content;
    @Column(nullable = false)
    private Integer version = 1;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ProtocolStatus status;
    @Column(nullable = false)
    private String authorId;
    @Column(nullable = false)
    private String authorName;
    @Column(nullable = false)
    private LocalDateTime createdAt;
    @Column
    private LocalDateTime updatedAt;
    @Column
    private String rejectedReason;
    @Column
    private LocalDateTime approvedAt;
    @Column
    private String approvedBy;

    @PrePersist
    public void prePersist() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        if (status == null) {
            status = ProtocolStatus.DRAFT;
        }
        if (version == null) {
            version = 1;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
