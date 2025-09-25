package pain_helper_back.nurse.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VasDTO {
    @Size(max = 25, message = "Pain place must be at most 25 characters long")
    private String painPlace;
    @NotNull(message = "Pain level is required")
    @Min(value = 0, message = "Pain level must be at least 0")
    @Max(value = 10, message = "Pain level must be at most 10")
    private Integer painLevel;
    private LocalDateTime createdAt = LocalDateTime.now();
}
