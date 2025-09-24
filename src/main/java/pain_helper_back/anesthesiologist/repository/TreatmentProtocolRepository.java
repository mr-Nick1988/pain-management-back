package pain_helper_back.anesthesiologist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.anesthesiologist.entity.TreatmentProtocol;
import pain_helper_back.enums.ProtocolStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentProtocolRepository extends JpaRepository<TreatmentProtocol, Long> {

    List<TreatmentProtocol> findByEscalationId(Long escalationId);

    List<TreatmentProtocol> findByStatus(ProtocolStatus status);

    List<TreatmentProtocol> findByAuthorId(String authorId);

    Optional<TreatmentProtocol> findTopByEscalationIdOrderByVersionDesc(Long escalationId);

    List<TreatmentProtocol> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByStatus(ProtocolStatus status);

    long countByAuthorId(String authorId);

    @Query("SELECT p FROM TreatmentProtocol p WHERE p.status = 'DRAFT' ORDER BY p.createdAt ASC")
    List<TreatmentProtocol> findPendingApproval();

    @Query("SELECT p FROM TreatmentProtocol p WHERE p.escalationId = :escalationId AND p.status = 'APPROVED' ORDER BY p.version DESC")
    List<TreatmentProtocol> findApprovedByEscalationId(@Param("escalationId") Long escalationId);

}
