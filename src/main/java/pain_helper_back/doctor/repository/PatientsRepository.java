package pain_helper_back.doctor.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.admin.entity.Person;
import pain_helper_back.doctor.entity.Patients;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientsRepository extends JpaRepository<Patients, Long> {
    boolean existsByEmrNumber(String emrNumber);
    Optional<Patients> findByEmrNumber(String emrNumber);
    List<Patients> findByCreatedBy(Person createdBy);
    List<Patients> findByActiveTrue();
    List<Patients> findByActiveTrueOrderByCreatedAtDesc();
}
