package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.common.patients.entity.Diagnosis;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    
//    /**
//     * Находит все diagnosisы для конкретной медицинской карты (EMR).
//     *
//     * @param emrId ID медицинской карты
//     * @return list diagnosisов
//     */
//    List<Diagnosis> findByEmrId(Long emrId);
//
//    /**
//     * Находит diagnosisы по ICD коду.
//     *
//     * @param IcdCode ICD код diagnosisа
//     * @return list diagnosisов с данным кодом
//     */
//    List<Diagnosis> findByICdCode(String IcdCode);
}
