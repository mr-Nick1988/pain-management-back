package pain_helper_back.doctor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.doctor.entity.AuditTrail;

import java.util.List;

@Repository
public interface AuditTrailRepository extends JpaRepository<AuditTrail, Long> {
    List<AuditTrail> findByPidOrderByTimestampDesc(Long pid);
}