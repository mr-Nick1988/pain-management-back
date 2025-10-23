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
    @Query("SELECT e FROM Escalation e WHERE e.recommendation.patient.mrn = :mrn")
    List<Escalation> findByRecommendationPatientMrn(@Param("mrn") String mrn);

    @Query("SELECT e FROM Escalation e WHERE e.recommendation.patient.mrn = :mrn ORDER BY e.createdAt DESC")
    Optional<Escalation> findTopByRecommendationPatientMrnOrderByCreatedAtDesc(@Param("mrn") String mrn);

    @Query("SELECT e FROM Escalation e WHERE e.status IN ('PENDING', 'IN_REVIEW') ORDER BY " +
            "CASE WHEN e.priority = 'CRITICAL' THEN 1 " +
            "WHEN e.priority = 'HIGH' THEN 2 " +
            "WHEN e.priority = 'MEDIUM' THEN 3 " +
            "WHEN e.priority = 'LOW' THEN 4 END, e.createdAt DESC")
    List<Escalation> findActiveEscalationsOrderedByPriorityAndDate();

}
