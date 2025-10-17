package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.common.patients.entity.Diagnosis;

import java.util.List;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    
    /**
     * Находит все диагнозы для конкретной медицинской карты (EMR).
     * 
     * @param emrId ID медицинской карты
     * @return список диагнозов
     */
    List<Diagnosis> findByEmrId(Long emrId);
    
    /**
     * Находит диагнозы по ICD коду.
     * 
     * @param icdCode ICD код диагноза
     * @return список диагнозов с данным кодом
     */
    List<Diagnosis> findByIcdCode(String icdCode);
}
