package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.common.patients.entity.Emr;

import java.util.List;

/**
 * Repository for медицинских карт (EMR).
 * Обычно all EMR тянутся via patient,
 * но иногда может понадобиться прямой доступ к ним.
 *
 * Exampleы кастомных requestов:
 * - Найти all EMR, где GFR < 60 (почечная недостаточность).
 * - Найти all EMR, где Child-Pugh = 'C' (тяжёлая печёночная недостаточность).
 * - Найти EMR patient по MRN.
 */
public interface EmrRepository extends JpaRepository<Emr, Long> {

    // по клиническим показателям
    List<Emr> findByGfrLessThan(String threshold);
    List<Emr> findByChildPughScore(String score);

    // доступ к EMR по бfromнес-identifierу patient (MRN)
    List<Emr> findByPatientMrn(String mrn);

    // вариант с сортировкой по дате создания (чтобы брать afterдние значения)
    List<Emr> findByPatientMrnOrderByCreatedAtDesc(String mrn);
}