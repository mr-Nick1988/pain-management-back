package pain_helper_back.anesthesiologist.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class CommentRequestDTO {
    @NotNull(message = "Protocol ID is required")
    private Long protocolId;
    
    @NotBlank(message = "Content is required")
    @Size(max = 2000, message = "Content must not exceed 2000 characters")
    private String content;
    
    @NotBlank(message = "Author ID is required")
    @Size(max = 50, message = "Author ID must not exceed 50 characters")
    private String authorId;
    
    @NotBlank(message = "Author name is required")
    @Size(max = 100, message = "Author name must not exceed 100 characters")
    private String authorName;
    
    private Boolean isQuestion = false;
}
