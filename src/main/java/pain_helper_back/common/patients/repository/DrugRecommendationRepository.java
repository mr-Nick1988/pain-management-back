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
 * - Найти все препараты для конкретного пациента по MRN.
 * - Найти все препараты по названию для пациента.
 */
public interface DrugRecommendationRepository extends JpaRepository<DrugRecommendation, Long> {

    // Все лекарства для пациента (JOIN через recommendation → patient → mrn)
    List<DrugRecommendation> findByRecommendationPatientMrn(String mrn);

    // Фильтрация по названию препарата (например, чтобы проверить дубли)
    List<DrugRecommendation> findByRecommendationPatientMrnAndDrugName(String mrn, String drugName);
}
