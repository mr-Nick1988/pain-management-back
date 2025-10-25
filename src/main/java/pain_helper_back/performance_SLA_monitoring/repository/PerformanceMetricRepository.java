package pain_helper_back.performance_SLA_monitoring.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pain_helper_back.performance_SLA_monitoring.entity.PerformanceMetric;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Репозиторий для метрик производительности в MongoDB
 */
@Repository
public interface PerformanceMetricRepository extends MongoRepository<PerformanceMetric, String> {

    /**
     * Найти все метрики за период
     */
    List<PerformanceMetric> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Найти все нарушения SLA
     */
    List<PerformanceMetric> findBySlaViolatedTrue();

    /**
     * Найти нарушения SLA за период
     */
    List<PerformanceMetric> findBySlaViolatedTrueAndTimestampBetween(LocalDateTime start, LocalDateTime end);

    /**
     * Найти метрики по операции
     */
    List<PerformanceMetric> findByOperationName(String operationName);

    /**
     * Найти метрики по операции за период
     */
    List<PerformanceMetric> findByOperationNameAndTimestampBetween(
            String operationName, LocalDateTime start, LocalDateTime end);

    /**
     * Найти метрики по пользователю
     */
    List<PerformanceMetric> findByUserId(String userId);

    /**
     * Найти метрики по пациенту
     */
    List<PerformanceMetric> findByPatientMrn(String patientMrn);

    /**
     * Найти метрики по статусу
     */
    List<PerformanceMetric> findByStatus(String status);

    /**
     * Средн время выполнения операции за период
     */
    @Query("{ 'operationName': ?0, 'timestamp': { $gte: ?1, $lte: ?2 } }")
    List<PerformanceMetric> findForAverageCalculation(
            String operationName, LocalDateTime start, LocalDateTime end);

    /**
     * Топ самых медленных операций
     */
    List<PerformanceMetric> findTop10ByOrderByExecutionTimeMsDesc();

    /**
     * Топ самых медленных операций за период
     */
    List<PerformanceMetric> findTop10ByTimestampBetweenOrderByExecutionTimeMsDesc(
            LocalDateTime start, LocalDateTime end);
}


