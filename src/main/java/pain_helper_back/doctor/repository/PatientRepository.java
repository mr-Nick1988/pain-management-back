package pain_helper_back.doctor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.doctor.entity.Patient;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    boolean existsByEmrNumber(String emrNumber);
    Optional<Patient> findByEmrNumber(String emrNumber);
    List<Patient> findByCreatedBy(Person createdBy);
    List<Patient> findByActiveTrue();
    List<Patient> findByActiveTrueOrderByCreatedAtDesc();
}
