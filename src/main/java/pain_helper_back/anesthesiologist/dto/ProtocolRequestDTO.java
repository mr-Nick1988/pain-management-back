package pain_helper_back.anesthesiologist.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Data
public class ProtocolRequestDTO {
    @NotNull(message = "Escalation ID is required")
    private Long escalationId;
    
    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title must not exceed 500 characters")
    private String title;
    
    @NotBlank(message = "Content is required")
    @Size(max = 5000, message = "Content must not exceed 5000 characters")
    private String content;
    
    @NotBlank(message = "Author ID is required")
    @Size(max = 50, message = "Author ID must not exceed 50 characters")
    private String authorId;
    
    @NotBlank(message = "Author name is required")
    @Size(max = 100, message = "Author name must not exceed 100 characters")
    private String authorName;
}
