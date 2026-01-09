package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.common.patients.entity.Diagnosis;

@Repository
public interface DiagnosisRepository extends JpaRepository<Diagnosis, Long> {
    
//    /**
//     * Находит all diagnosisы for конкретной медицинской карты (EMR).
//     *
//     * @param emrId ID медицинской карты
//     * @return list diagnosisов
//     */
//    List<Diagnosis> findByEmrId(Long emrId);
//
//    /**
//     * Находит diagnosisы по ICD codeу.
//     *
//     * @param IcdCode ICD code diagnosisа
//     * @return list diagnosisов с данным codeом
//     */
//    List<Diagnosis> findByICdCode(String IcdCode);
}
