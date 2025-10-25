package pain_helper_back.common.patients.repository;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import pain_helper_back.common.patients.entity.Patient;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Основной репозиторий для работы с пациентами.
 * Через него можно добраться до всех связанных сущностей (EMR, VAS, Recommendations).
 */
public interface PatientRepository extends JpaRepository<Patient, Long>, JpaSpecificationExecutor<Patient> {


    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPhoneNumber(String phoneNumber);

    Optional<Patient> findByMrn(String mrn);

    void deleteByMrn(String mrn);

    boolean existsByPhoneNumber(String phoneNumber);

    boolean existsByEmail(String email);


    List<Patient> getPatientsByFirstNameAndLastName(String firstName, String lastName);

    List<Patient> findByIsActive(Boolean isActive);

    List<Patient> findByDateOfBirth(LocalDate dateOfBirth);

    @Override
    @EntityGraph(attributePaths = "vas")
    List<Patient> findAll();
}

