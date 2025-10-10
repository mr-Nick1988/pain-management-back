package pain_helper_back.analytics.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import pain_helper_back.analytics.entity.PerformanceMetric;

import java.time.LocalDateTime;
import java.util.List;

/*
 * Repository для работы с метриками производительности в MongoDB
 */
@Repository
public interface PerformanceMetricRepository extends MongoRepository<PerformanceMetric, String> {
    //search by module

    List<PerformanceMetric> findByModule(String module);

    // find by performance category
    List<PerformanceMetric> findByPerformanceCategory(String performanceCategory);

    //find by metrics that need attention
    List<PerformanceMetric> findByRequiresAttentionTrue();

    // find by timestamp between
    List<PerformanceMetric> findByTimestampBetween(LocalDateTime start, LocalDateTime end);

    // find by slow operations (more than a given type)
    @Query("{ 'executionTimeMs': { $gt: ?0 } }")
    List<PerformanceMetric> findSlowOperations(Long thresholdMs);

    // top N most slow operations
    @Query(value = "{}", sort = "{ 'executionTimeMs': -1 }")
    List<PerformanceMetric> findTopSlowOperations();

    // average time execution of the method
    @Query(value = "{ 'methodName': ?0 }", fields = "{ 'executionTimeMs': 1 }")
    List<PerformanceMetric> findByMethodName(String methodName);

    // calculate slow operation by module
    Long countByModuleAndPerformanceCategory(String module, String performanceCategory);
}
