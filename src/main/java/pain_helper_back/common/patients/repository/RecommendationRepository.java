package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.RecommendationStatus;

import java.util.List;

/**
 * Репозиторий для рекомендаций.
 * Обычно их тянут через пациента,
 * но иногда нужен прямой доступ.
 *
 * Примеры кастомных запросов:
 * - Найти все рекомендации по MRN пациента.
 * - Найти последние рекомендации по MRN.
 * - Найти все рекомендации по статусу (например, только PENDING).
 */
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {



    // по бизнес-идентификатору (MRN)
    List<Recommendation> findByPatientMrn(String mrn);

    // сортировка по дате обновления (например, чтобы взять свежие рекомендации)
    List<Recommendation> findByPatientMrnOrderByUpdatedAtDesc(String mrn);



    // поиск по статусу
    List<Recommendation> findByStatus(RecommendationStatus status);

    // поиск по MRN и статусу
   List<Recommendation> findByPatientMrnAndStatus(String mrn, RecommendationStatus status);
}
