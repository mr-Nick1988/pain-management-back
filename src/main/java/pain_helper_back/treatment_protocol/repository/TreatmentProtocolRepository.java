package pain_helper_back.treatment_protocol.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.treatment_protocol.entity.TreatmentProtocol;

import java.util.List;

@Repository
public interface TreatmentProtocolRepository extends JpaRepository<TreatmentProtocol, Long> {
//    List<TreatmentProtocol> findByPainLevel(String painLevel);
}