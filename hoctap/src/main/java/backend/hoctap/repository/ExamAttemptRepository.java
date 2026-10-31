package backend.hoctap.repository;

import backend.hoctap.entity.ExamAttempt;
import backend.hoctap.entity.ExamAttempt.AttemptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ExamAttemptRepository extends JpaRepository<ExamAttempt, Long> {

    // ── Lịch sử làm bài theo user ─────────────────────────────────────────

    List<ExamAttempt> findByUserIdOrderByStartedAtDesc(Long userId);

    List<ExamAttempt> findByUserIdAndStatus(Long userId, AttemptStatus status);

    // ── Bảng xếp hạng theo kỳ (tuần/tháng) ────────────────────────────────

    /** Tổng xu kiếm được từ bài thi của MỌI user, kể từ mốc thời gian (group theo user) */
    @Query("""
            SELECT a.user.id, COALESCE(SUM(a.coinsEarned), 0)
            FROM ExamAttempt a
            WHERE a.status <> :status
              AND a.submittedAt >= :start
            GROUP BY a.user.id
            """)
    List<Object[]> sumCoinsEarnedSince(@Param("start") LocalDateTime start,
                                       @Param("status") AttemptStatus status);

    // ── Lần làm bài theo bài thi ──────────────────────────────────────────

    List<ExamAttempt> findByExamIdOrderByScoreDesc(Long examId);

    long countByExamId(Long examId);

    // ── Kiểm tra user đã làm bài thi này chưa ────────────────────────────

    boolean existsByUserIdAndExamIdAndStatus(Long userId, Long examId, AttemptStatus status);

    // ── Điểm cao nhất của user trong một bài thi ──────────────────────────

    @Query("""
            SELECT a FROM ExamAttempt a
            WHERE a.user.id = :userId
              AND a.exam.id = :examId
              AND a.status  = 'COMPLETED'
            ORDER BY a.score DESC
            LIMIT 1
            """)
    Optional<ExamAttempt> findBestAttempt(
            @Param("userId") Long userId,
            @Param("examId") Long examId
    );

    // ── Tổng xu kiếm được từ các bài thi ─────────────────────────────────

    @Query("SELECT COALESCE(SUM(a.coinsEarned), 0) FROM ExamAttempt a WHERE a.user.id = :userId")
    int sumCoinsByUserId(@Param("userId") Long userId);

    // ── Số bài thi đã hoàn thành ──────────────────────────────────────────

    long countByUserIdAndStatus(Long userId, AttemptStatus status);

    // ── Lần làm bài đang tiến hành (chưa nộp) ────────────────────────────

    Optional<ExamAttempt> findByUserIdAndExamIdAndStatus(
            Long userId, Long examId, AttemptStatus status);
}
