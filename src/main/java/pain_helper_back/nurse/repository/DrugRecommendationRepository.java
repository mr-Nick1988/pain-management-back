package pain_helper_back.nurse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.nurse.entity.DrugRecommendation;

import java.util.List;

public interface DrugRecommendationRepository extends JpaRepository<DrugRecommendation, Long> {

}
