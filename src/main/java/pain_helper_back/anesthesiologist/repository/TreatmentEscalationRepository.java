package pain_helper_back.anesthesiologist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.anesthesiologist.entity.Escalation;
import pain_helper_back.enums.EscalationPriority;
import pain_helper_back.enums.EscalationStatus;

import java.util.List;

@Repository
public interface TreatmentEscalationRepository extends JpaRepository<Escalation, Long> {
    List<Escalation> findAllByEscalationStatus(EscalationStatus status);

    List<Escalation> findByEscalationPriority(EscalationPriority priority);

    List<Escalation> findAllByEscalationPriorityAndEscalationStatus(EscalationPriority priority, EscalationStatus status);

    List<Escalation> findByPatientId(String patientId);

    List<Escalation> findByDoctorName(String doctorName);

    Long countByEscalationStatus(EscalationStatus status);

    Long countByEscalationPriority(EscalationPriority priority);

    @Query("SELECT e FROM Escalation e WHERE e.escalationStatus = :status ORDER BY e.escalationPriority DESC, e.createdAt ASC")
    List<Escalation> findByEscalationStatusOrderByCreatedAtDesc(@Param("status") EscalationStatus status);

    @Query("SELECT e FROM Escalation e WHERE e.escalationPriority= 'CRITICAL' AND e.escalationStatus IN('PENDING', 'IN_REVIEW')")
    List<Escalation>findCriticalActiveEscalations();

    @Query("SELECT COUNT(e) FROM Escalation e WHERE e.escalationStatus = :status")
    Long countByStatus(@Param("status") EscalationStatus status);

    @Query("SELECT COUNT(e) FROM Escalation e WHERE e.escalationPriority = :priority")
    Long countByPriority(@Param("priority") EscalationPriority priority);
}
