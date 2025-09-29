package pain_helper_back.nurse.repository;

import jakarta.validation.constraints.NotBlank;
import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.nurse.entity.Patient;

import java.util.List;
import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Long> {

    boolean existsByMrn(String mrn);

    Optional<Patient> findByMrn(String mrn);

    void deleteByMrn(String mrn);

    boolean existsByPhoneNumber(String phoneNumber);
    boolean existsByEmail(String email);

    Optional<Patient> findByEmail(String email);
    Optional<Patient> findByPhoneNumber(String phoneNumber);
    List<Patient> getPatientsByFirstNameAndLastName(String firstName, String lastName);

}
