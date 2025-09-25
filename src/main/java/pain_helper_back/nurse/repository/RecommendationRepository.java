package pain_helper_back.nurse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.anesthesiologist.entity.Recommendation;

import java.util.List;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

}
