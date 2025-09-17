package pain_helper_back.admin.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.admin.entity.Person;

import java.util.Optional;

public interface PersonRepository extends JpaRepository<Person, Long> {
    Optional<Person> findByLogin(String login);
    boolean existsByLogin(String login);
    boolean existsByPersonId(String documentId);
    Optional<Person> findByPersonId(String documentId);
}

