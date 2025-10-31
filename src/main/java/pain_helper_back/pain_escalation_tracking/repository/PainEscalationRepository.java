package pain_helper_back.pain_escalation_tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.pain_escalation_tracking.entity.PainEscalation;

@Repository
public interface PainEscalationRepository extends JpaRepository<PainEscalation, Long> {

}
