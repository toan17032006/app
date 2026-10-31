package backend.hoctap.repository;

import backend.hoctap.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // ── Lấy câu hỏi theo bài thi ──────────────────────────────────────────

    List<Question> findByExamIdOrderByOrderIndexAsc(Long examId);

    int countByExamId(Long examId);

    // ── Lấy kèm đáp án (tránh N+1) ───────────────────────────────────────

    @Query("""
            SELECT DISTINCT q FROM Question q
            LEFT JOIN FETCH q.options
            WHERE q.exam.id = :examId
            ORDER BY q.orderIndex ASC
            """)
    List<Question> findByExamIdWithOptions(@Param("examId") Long examId);

    // ── Xóa toàn bộ câu hỏi của một bài thi ──────────────────────────────

    void deleteByExamId(Long examId);
}
