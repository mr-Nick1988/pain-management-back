package pain_helper_back.nurse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.nurse.entity.Patient;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByPersonId(String personId);
    Optional<Patient> findByPersonId(String personId);

    void deleteByPersonId(String personId);
}
