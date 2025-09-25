package pain_helper_back.doctor.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.doctor.entity.Patients;
import pain_helper_back.doctor.entity.Recommendation;
import pain_helper_back.enums.RecommendationStatus;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationsRepository extends JpaRepository<Recommendation, Long> {
    List<Recommendation> findByPatients(Patients patients);
    List<Recommendation> findByStatus(RecommendationStatus status);
    List<Recommendation> findByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    List<Recommendation> findByCreatedBy(Person person);
    List<Recommendation> findByUpdatedBy(Person person);
    List<Recommendation> findAllByOrderByCreatedAtDesc();
    List<Recommendation> findByStatusOrderByUpdatedAtDesc(RecommendationStatus status);
}
