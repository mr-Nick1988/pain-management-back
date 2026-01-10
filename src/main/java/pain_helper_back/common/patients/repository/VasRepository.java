package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.common.patients.entity.Vas;

import java.util.List;

/**
 * Repository for жалоб patient (VAS – pain score).
 * via Patient можно получить all жалобы,
 * но иногда нужны выборки по allм patientм.
 *
 * Exampleы кастомных requestов:
 * - Найти all жалобы, где painLevel > 7 (сильная pain).
 * - Найти afterдние жалобы по mrn (ORDER BY createdAt DESC).
 */
public interface VasRepository extends JpaRepository<Vas, Long> {
    List<Vas> findByPainLevelGreaterThan(int threshold);
    List<Vas> findByPatientMrn(String mrn);
    List<Vas> findByPatientMrnOrderByCreatedAtDesc(String mrn);
}
