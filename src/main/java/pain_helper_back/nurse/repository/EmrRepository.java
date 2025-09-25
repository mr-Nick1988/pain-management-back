package pain_helper_back.nurse.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import pain_helper_back.nurse.entity.Emr;

import java.util.List;

public interface EmrRepository extends JpaRepository<Emr,Long> {


}
