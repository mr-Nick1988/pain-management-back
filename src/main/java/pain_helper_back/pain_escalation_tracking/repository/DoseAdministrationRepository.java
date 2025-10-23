package pain_helper_back.pain_escalation_tracking.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.common.patients.entity.Patient;
import pain_helper_back.pain_escalation_tracking.entity.DoseAdministration;

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

    List<DoseAdministration> findByPatientOrderByAdministeredAtDesc(Patient patient);

    Optional<DoseAdministration> findTopByPatientOrderByAdministeredAtDesc(Patient patient);
}
