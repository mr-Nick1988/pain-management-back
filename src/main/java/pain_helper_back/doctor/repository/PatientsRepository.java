package pain_helper_back.doctor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.doctor.entity.Patients;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PatientsRepository extends JpaRepository<Patients, Long> {
    boolean existsByMRN(String mrn);
    Optional<Patients> findByMRN(String mrn);
    Optional<Patients> findByFirstNameAndLastNameAndDateOfBirth(String firstName, String lastName, LocalDate dateOfBirth);
    Optional<Patients> findByFirstNameAndLastNameAndDateOfBirthAndInsurancePolicyNumber(String firstName, String lastName, LocalDate dateOfBirth, String insurancePolicyNumber);
    List<Patients> findByInsurancePolicyNumber(String insurancePolicyNumber);

   // List<Patients> findByCreatedBy(Person createdBy);
   // List<Patients> findByActiveTrue();
    List<Patients> findByActiveTrueOrderByCreatedAtDesc();
}