package pain_helper_back.common.patients.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class EmrUpdateDTO {
    private Double weight;
    private Double height;
    private String gfr;
    private String childPughScore;
    private Double plt;
    private Double wbc;
    private Double sat;
    private Double sodium;
    private LocalDateTime updatedAt;
    private String updatedBy;
}
