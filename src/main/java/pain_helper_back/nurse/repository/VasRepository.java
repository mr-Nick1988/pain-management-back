package pain_helper_back.nurse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.nurse.entity.Vas;

import java.util.List;

public interface VasRepository extends JpaRepository<Vas, Long> {

}
