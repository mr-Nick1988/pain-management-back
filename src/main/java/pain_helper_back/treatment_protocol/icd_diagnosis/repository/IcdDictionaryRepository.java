package pain_helper_back.treatment_protocol.icd_diagnosis.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.treatment_protocol.icd_diagnosis.entity.IcdDictionary;

import java.util.List;

public interface IcdDictionaryRepository extends JpaRepository<IcdDictionary, String> {
    List<IcdDictionary> findTop20ByCodeStartingWithIgnoreCase(String q);
    List<IcdDictionary> findTop20ByDescriptionContainingIgnoreCase(String q);
}
