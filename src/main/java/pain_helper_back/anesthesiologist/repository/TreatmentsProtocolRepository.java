package pain_helper_back.anesthesiologist.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.anesthesiologist.entity.TreatmentsProtocol;
import pain_helper_back.enums.ProtocolStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentsProtocolRepository extends JpaRepository<TreatmentsProtocol, Long> {

    List<TreatmentsProtocol> findByEscalationId(Long escalationId);

    List<TreatmentsProtocol> findByStatus(ProtocolStatus status);

    List<TreatmentsProtocol> findByAuthorId(String authorId);

    Optional<TreatmentsProtocol> findTopByEscalationIdOrderByVersionDesc(Long escalationId);

    List<TreatmentsProtocol> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByStatus(ProtocolStatus status);

    long countByAuthorId(String authorId);

    @Query("SELECT p FROM TreatmentsProtocol p WHERE p.status = 'DRAFT' ORDER BY p.createdAt ASC")
    List<TreatmentsProtocol> findPendingApproval();

    @Query("SELECT p FROM TreatmentsProtocol p WHERE p.escalationId = :escalationId AND p.status = 'APPROVED' ORDER BY p.version DESC")
    List<TreatmentsProtocol> findApprovedByEscalationId(@Param("escalationId") Long escalationId);

}
