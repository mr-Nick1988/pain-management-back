package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.common.patients.entity.DrugRecommendation;

import java.util.List;

/**
 * Репозиторий для лекарственных назначений внутри рекомендаций.
 * Обычно тянется через Recommendation,
 * но можно делать прямые запросы.
 *
 * Примеры кастомных запросов:
 * - Найти все drugы для конкретного patient по MRN.
 * - Найти все drugы по названию для patient.
 */
public interface DrugRecommendationRepository extends JpaRepository<DrugRecommendation, Long> {

    // Все лекарства для patient (JOIN через recommendation → patient → mrn)
    List<DrugRecommendation> findByRecommendationPatientMrn(String mrn);

    // Фильтрация по названию drugа (например, чтобы проверить дубли)
    List<DrugRecommendation> findByRecommendationPatientMrnAndDrugName(String mrn, String drugName);
}
