package pain_helper_back.treatment_protocol.icd_diagnosis.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "icd_dictionary")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class IcdDictionary {

    @Id
    @JsonProperty("icdCode")   // 👈 добавляем JSON-алиас для соответствия с фронтом
    private String code;        // "E11.9"

    private String description; // "Type 2 diabetes mellitus..."
}