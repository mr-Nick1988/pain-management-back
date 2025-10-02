package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.common.patients.entity.Emr;

import java.util.List;

/**
 * Репозиторий для медицинских карт (EMR).
 * Обычно все EMR тянутся через пациента,
 * но иногда может понадобиться прямой доступ к ним.
 *
 * Примеры кастомных запросов:
 * - Найти все EMR, где GFR < 60 (почечная недостаточность).
 * - Найти все EMR, где Child-Pugh = 'C' (тяжёлая печёночная недостаточность).
 * - Найти EMR пациента по MRN.
 */
public interface EmrRepository extends JpaRepository<Emr, Long> {

    // по клиническим показателям
    List<Emr> findByGfrLessThan(String threshold);
    List<Emr> findByChildPughScore(String score);

    // доступ к EMR по бизнес-идентификатору пациента (MRN)
    List<Emr> findByPatientMrn(String mrn);

    // вариант с сортировкой по дате создания (чтобы брать последние значения)
    List<Emr> findByPatientMrnOrderByCreatedAtDesc(String mrn);
}