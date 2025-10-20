package pain_helper_back.reporting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import pain_helper_back.reporting.entity.MonthlyReportAggregate;

import java.util.List;
import java.util.Optional;

@Repository
public interface MonthlyReportRepository extends JpaRepository<MonthlyReportAggregate, Long> {
    /*
     * Найти отчет за конкретный месяц
     * @param reportMonth Формат: "2025-10"
     */
    Optional<MonthlyReportAggregate> findByReportMonth(String reportMonth);

    boolean existsByReportMonth(String reportMonth);

    @Query("SELECT m FROM MonthlyReportAggregate m ORDER BY m.reportMonth DESC")
    List<MonthlyReportAggregate> findRecentReports();

    /**
     * Найти отчеты за последние N месяцев
     */
    @Query(value = "SELECT * FROM monthly_report_aggregates " +
            "ORDER BY report_month DESC LIMIT ?1",
            nativeQuery = true)
    List<MonthlyReportAggregate> findLastNMonths(int n);
}
