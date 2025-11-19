package pain_helper_back.reporting.repository;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import pain_helper_back.reporting.entity.DailyReportAggregate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReportAggregate, Long> {

    Optional<DailyReportAggregate> findByReportDate(LocalDate reportDate);

    boolean existsByReportDate(LocalDate reportDate);

    List<DailyReportAggregate> findAllByReportDateBetween(LocalDate startDate, LocalDate endDate);

    List<DailyReportAggregate> findByReportDateGreaterThanEqual(LocalDate startDate);

    List<DailyReportAggregate> findAllByOrderByReportDateDesc(Pageable pageable);
}
