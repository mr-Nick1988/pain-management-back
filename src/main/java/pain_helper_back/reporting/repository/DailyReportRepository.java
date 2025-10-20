package pain_helper_back.reporting.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.reporting.entity.DailyReportAggregate;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyReportRepository extends JpaRepository<DailyReportAggregate, Long> {
    /**
     * Найти отчет за конкретную дату
     */
    Optional<DailyReportAggregate> findByReportDate(LocalDate date);

    /**
     * Найти отчеты за диапазон дат
     */
    List<DailyReportAggregate> findByReportDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Найти последние N отчетов
     */
    @Query("SELECT d FROM DailyReportAggregate d ORDER BY d.reportDate DESC")
    List<DailyReportAggregate> findRecentReports(@Param("limit") int limit);

    /**
     * Проверить, существует ли отчет за дату
     */
    boolean existsByReportDate(LocalDate date);

    /**
     * Получить отчеты за последние N дней
     */
    @Query("SELECT d FROM DailyReportAggregate d " +
            "WHERE d.reportDate >= :startDate " +
            "ORDER BY d.reportDate DESC")
    List<DailyReportAggregate> findReportsForLastDays(@Param("startDate")LocalDate startDate);

}
