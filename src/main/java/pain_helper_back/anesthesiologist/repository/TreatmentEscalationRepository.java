package pain_helper_back.anesthesiologist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.util.List;
import java.util.Optional;


/*
 * Repository для работы с эскалациями
 * Обновлен под новую структуру Escalation с полями: status, priority, recommendation
 */

@Repository
public interface TreatmentEscalationRepository extends JpaRepository<Escalation, Long> {
    // ========== ПОИСК ПО СТАТУСУ ========== //
    List<Escalation> findByStatus(EscalationStatus status);
    Long countByStatus(EscalationStatus status);
    // ========== ПОИСК ПО ПРИОРИТЕТУ ========== //
    List<Escalation> findByPriority(EscalationPriority priority);
    Long countByPriority(EscalationPriority priority);
    // ========== КОМБИНИРОВАННЫЙ ПОИСК ========== //
    List<Escalation>findByPriorityAndStatus(EscalationPriority priority, EscalationStatus status);
    // ========== ПОИСК ПО ВРАЧУ ========== //
    List<Escalation>findByEscalatedBy(String escalatedBy);
    // ========== ПОИСК ПО АНЕСТЕЗИОЛОГУ ========== //
    //Найти все эскалации, разрешенные конкретным анестезиологом
    List<Escalation> findByResolvedBy(String resolvedBy);
    // ========== ПОИСК ПО РЕКОМЕНДАЦИИ ========== //
    //Найти эскалацию по ID рекомендации
    @Query("SELECT e FROM Escalation e WHERE e.recommendation.id = :recommendationId")
    Optional<Escalation>findByRecommendationId(@Param("recommendationId") Long recommendationId);


    // ========== СЛОЖНЫЕ ЗАПРОСЫ ========== //
    /*
     * Найти все активные (PENDING, IN_PROGRESS) эскалации, отсортированные по приоритету и дате
     * Сначала HIGH, потом MEDIUM, потом LOW
     * Внутри каждого приоритета - сначала старые
     */
    @Query("SELECT e FROM Escalation e WHERE e.status IN ('PENDING', 'IN_PROGRESS') " +
            "ORDER BY CASE e.priority " +
            "WHEN 'HIGH' THEN 1 " +
            "WHEN 'MEDIUM' THEN 2 " +
            "WHEN 'LOW' THEN 3 END, " +
            "e.createdAt ASC")
    List<Escalation> findActiveEscalationsOrderedByPriorityAndDate();

    /*
     * Найти критические (HIGH priority) активные эскалации
     * @return список критических эскалаций
     */
    @Query("SELECT e FROM Escalation e WHERE e.priority = 'HIGH' AND e.status IN ('PENDING', 'IN_PROGRESS')")
    List<Escalation> findCriticalActiveEscalations();

    /*
     * Подсчет всех эскалаций
     * @return общее количество
     */
    @Query("SELECT COUNT(e) FROM Escalation e")
    Long countAll();
}
