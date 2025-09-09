package pain_helper_back.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.entity.User;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
}
