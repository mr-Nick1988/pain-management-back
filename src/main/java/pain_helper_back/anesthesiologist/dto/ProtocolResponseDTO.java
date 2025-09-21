package pain_helper_back.anesthesiologist.dto;

import lombok.Data;
import pain_helper_back.enums.ProtocolStatus;

import java.time.LocalDateTime;

@Data
public class ProtocolResponseDTO {
    private Long id;
    private Long escalationId;
    private String title;
    private String content;
    private Integer version;
    private ProtocolStatus status;
    private String authorId;
    private String authorName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String rejectedReason;
    private LocalDateTime approvedAt;
    private String approvedBy;
}
