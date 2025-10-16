//package pain_helper_back.analytics.repository;
//
//import org.springframework.data.mongodb.repository.MongoRepository;
//import org.springframework.data.mongodb.repository.Query;
//import org.springframework.stereotype.Repository;
//import pain_helper_back.analytics.entity.LogEntry;
//
//
//import java.time.LocalDateTime;
//import java.util.List;
//
///*
// * Repository для работы с техническими логами в MongoDB
// */
//@Repository
//public interface LogEntryRepository extends MongoRepository<LogEntry, String> {
//
//    //find by module
//    List<LogEntry> findByModule(String module);
//
//    //logCategory search
//    List<LogEntry> findByLogCategory(String logCategory);
//
//    //find by error
//    List<LogEntry> findBySuccessFalse(String error);
//
//    //find by timestamp between
//    List<LogEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
//
//    //find of slow operations(more than 1000ms)
//    @Query("{'durationMs': {$gt: ?0}}")
//    List<LogEntry> findSlowOperations(Long thresholdMs);
//
//    //find by module and timestamp between
//    List<LogEntry> findByModuleAndTimestampBetween(String module, LocalDateTime start, LocalDateTime end);
//
//    //errors calculation y module
//    Long countByModuleAndSuccessFalse(String module);
//
//    List<LogEntry>findByLevelAndTimestampBetween(String level, LocalDateTime startDate, LocalDateTime endDate);
//
//    List<LogEntry>findByLevel(String level);
//}
