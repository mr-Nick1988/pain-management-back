package pain_helper_back.reporting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.reporting.entity.WeeklyReportAggregate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface WeeklyReportRepository extends JpaRepository<WeeklyReportAggregate, Long> {
    Optional<WeeklyReportAggregate> findByWeekStartDate(LocalDate weekStartDate);

    List<WeeklyReportAggregate> findByWeekStartDateBetween(LocalDate startDate, LocalDate endDate);

    boolean existsByWeekStartDate(LocalDate weekStartDate);

    @Query("SELECT w FROM WeeklyReportAggregate w ORDER BY w.weekStartDate DESC")
    List<WeeklyReportAggregate> findRecentReports(@Param("limit") int limit);
}
