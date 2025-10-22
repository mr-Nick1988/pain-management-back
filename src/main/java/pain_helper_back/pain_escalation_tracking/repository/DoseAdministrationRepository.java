package pain_helper_back.pain_escalation_tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.pain_escalation_tracking.entity.DoseAdministration;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoseAdministrationRepository extends JpaRepository<DoseAdministration, Long> {
    /**
     * Найти последнюю введенную дозу для пациента
     */
    @Query("SELECT d FROM DoseAdministration d WHERE d.patient.mrn = :mrn " +
            "ORDER BY d.administeredAt DESC")
    Optional<DoseAdministration> findLastDoseByPatientMrn(@Param("mrn") String mrn);

    /**
     * Найти все дозы пациента за период
     */
    @Query("SELECT d FROM DoseAdministration d WHERE d.patient.mrn = :mrn " +
            "AND d.administeredAt >= :startTime ORDER BY d.administeredAt DESC")
    List<DoseAdministration> findDosesByPatientMrnAndPeriod(
            @Param("mrn") String mrn,
            @Param("startTime") LocalDateTime startTime);

    /**
     * Подсчитать количество доз за период
     */
    @Query("SELECT COUNT(d) FROM DoseAdministration d WHERE d.patient.mrn = :mrn " +
            "AND d.administeredAt >= :startTime")
    long countDosesByPatientMrnAndPeriod(
            @Param("mrn") String mrn,
            @Param("startTime") LocalDateTime startTime);
}
