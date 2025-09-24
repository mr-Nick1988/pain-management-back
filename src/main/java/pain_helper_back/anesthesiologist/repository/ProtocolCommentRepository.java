package pain_helper_back.anesthesiologist.repository;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import pain_helper_back.anesthesiologist.entity.TreatmentProtocolComment;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ProtocolCommentRepository extends JpaRepository<TreatmentProtocolComment, Long> {

    List<TreatmentProtocolComment> findByProtocolId(Long protocolId);

    List<TreatmentProtocolComment> findByProtocolIdOrderByCreatedAtAsc(Long protocolId);

    List<TreatmentProtocolComment> findByAuthorId(String authorId);

    List<TreatmentProtocolComment> findByProtocolIdAndAuthorId(Long protocolId, String authorId);

    List<TreatmentProtocolComment> findByProtocolIdAndIsQuestionTrue(Long protocolId);

    List<TreatmentProtocolComment> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByProtocolId(Long protocolId);

    long countByProtocolIdAndIsQuestionTrue(Long protocolId);

    long countByAuthorId(String authorId);

    @Query("SELECT pc FROM TreatmentProtocolComment pc WHERE pc.protocolId = :protocolId ORDER BY pc.createdAt DESC")
    List<TreatmentProtocolComment> findLatestByProtocolId(@Param("protocolId") Long protocolId);

    //find unanswered questions (вопросы без последующих комментариев от других пользователей)
    @Query("SELECT pc FROM TreatmentProtocolComment pc WHERE pc.protocolId = :protocolId AND pc.isQuestion = true " +
            "AND NOT EXISTS (SELECT pc2 FROM TreatmentProtocolComment pc2 WHERE pc2.protocolId = :protocolId " +
            "AND pc2.authorId != pc.authorId AND pc2.createdAt > pc.createdAt)")
    List<TreatmentProtocolComment> findUnansweredQuestions(@Param("protocolId") Long protocolId);
}
