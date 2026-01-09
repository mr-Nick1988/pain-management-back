package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.common.patients.entity.DrugRecommendation;

import java.util.List;

/**
 * Repository for лекарственных назначений внутри рекомендаций.
 * Обычно тянется via Recommendation,
 * но можно делать прямые requestы.
 *
 * Exampleы кастомных requestов:
 * - Найти all drugы for конкретного patient по MRN.
 * - Найти all drugы по названию for patient.
 */
public interface DrugRecommendationRepository extends JpaRepository<DrugRecommendation, Long> {

    // all лекарства for patient (JOIN via recommendation → patient → mrn)
    List<DrugRecommendation> findByRecommendationPatientMrn(String mrn);

    // Фильтрация по названию drugа (наExample, чтобы проверить дубли)
    List<DrugRecommendation> findByRecommendationPatientMrnAndDrugName(String mrn, String drugName);
}
