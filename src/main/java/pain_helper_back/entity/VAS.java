package pain_helper_back.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity

@Data
public class VAS {
    @Id
    private Long id;
    private Long patientId;
    private Integer painLevel; // 0-10 scale
    private LocalDateTime timestamp;
}
