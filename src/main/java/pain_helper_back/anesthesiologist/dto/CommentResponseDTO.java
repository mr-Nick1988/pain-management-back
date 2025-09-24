package pain_helper_back.anesthesiologist.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CommentResponseDTO {
    private Long id;
    private Long protocolId;
    private String content;
    private String authorId;
    private String authorName;
    private Boolean isQuestion;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
