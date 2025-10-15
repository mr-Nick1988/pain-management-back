package pain_helper_back.treatment_protocol.icd_diagnosis.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name="icd_dictionary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IcdDictionary {
    @Id
    private String code;        // "E11.9"
    private String description;     // "Type 2 diabetes mellitus..."

}