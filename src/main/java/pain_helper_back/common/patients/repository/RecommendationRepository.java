package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.common.patients.dto.RecommendationWithVasDTO;
import pain_helper_back.common.patients.entity.Recommendation;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for рекомендаций.
 * Обычно их тянут via patient,
 * но иногда нужен прямой доступ.
 *
 * Exampleы кастомных requestов:
 * - Найти all recommendation по MRN patient.
 * - Найти afterдние recommendation по MRN.
 * - Найти all recommendation по statusу (наExample, только PENDING).
 */
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {



    // по бfromнес-identifierу (MRN)
    List<Recommendation> findByPatientMrn(String mrn);

    // сортировка по дате обновления (наExample, чтобы взять свежие recommendation)
    List<Recommendation> findByPatientMrnOrderByUpdatedAtDesc(String mrn);



    // поиск по statusу
    List<Recommendation> findByStatus(RecommendationStatus status);

    // поиск по MRN и statusу
   List<Recommendation> findByPatientMrnAndStatus(String mrn, RecommendationStatus status);


    Optional<Recommendation> findTopByPatientMrnAndStatusOrderByCreatedAtDesc(
            String mrn,
            RecommendationStatus status
    );

    List<Recommendation> findAllByCreatedAtAfter(LocalDateTime since);
}
