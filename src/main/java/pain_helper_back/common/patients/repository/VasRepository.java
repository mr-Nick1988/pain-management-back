package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.common.patients.entity.Vas;

import java.util.List;

/**
 * Репозиторий для жалоб пациента (VAS – pain score).
 * Через Patient можно получить все жалобы,
 * но иногда нужны выборки по всем пациентам.
 *
 * Примеры кастомных запросов:
 * - Найти все жалобы, где painLevel > 7 (сильная боль).
 * - Найти последние жалобы по mrn (ORDER BY createdAt DESC).
 */
public interface VasRepository extends JpaRepository<Vas, Long> {
    List<Vas> findByPainLevelGreaterThan(int threshold);
    List<Vas> findByPatientMrn(String mrn);
    List<Vas> findByPatientMrnOrderByCreatedAtDesc(String mrn);
}
