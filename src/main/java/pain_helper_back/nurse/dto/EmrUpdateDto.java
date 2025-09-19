package pain_helper_back.nurse.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class EmrUpdateDto {
    private String gfr;
    private String childPughScore;
    private Double plt;
    private Double wbc;
    private Double sat;
    private Double sodium;
    private LocalDate createdAt = LocalDate.now();
}
