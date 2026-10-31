package backend.hoctap.repository;

import backend.hoctap.entity.Homework;
import backend.hoctap.entity.Homework.HomeworkStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HomeworkRepository extends JpaRepository<Homework, Long> {

    // ── Bảng xếp hạng theo kỳ (tuần/tháng) ────────────────────────────────

    /** Tổng xu kiếm được từ nộp bài tập của MỌI user, kể từ mốc thời gian (group theo user) */
    @Query("""
            SELECT h.user.id, COALESCE(SUM(h.coinsEarned), 0)
            FROM Homework h
            WHERE h.submittedAt >= :start
            GROUP BY h.user.id
            """)
    List<Object[]> sumHomeworkCoinsSince(@Param("start") LocalDateTime start);

    // ── Lấy toàn bộ bài tập của user ─────────────────────────────────────

    List<Homework> findByUserIdOrderByDueDateAsc(Long userId);

    // ── Lọc theo trạng thái ───────────────────────────────────────────────

    List<Homework> findByUserIdAndStatus(Long userId, HomeworkStatus status);

    // ── Lọc theo môn học ──────────────────────────────────────────────────

    List<Homework> findByUserIdAndSubject(Long userId, String subject);

    // ── Bài tập sắp hết hạn (trong N ngày tới) ───────────────────────────

    @Query("""
            SELECT h FROM Homework h
            WHERE h.user.id = :userId
              AND h.status  = 'PENDING'
              AND h.dueDate <= :deadline
            ORDER BY h.dueDate ASC
            """)
    List<Homework> findUpcomingDue(
            @Param("userId")   Long userId,
            @Param("deadline") LocalDate deadline
    );

    // ── Bài tập đã quá hạn (chưa nộp) ───────────────────────────────────

    @Query("""
            SELECT h FROM Homework h
            WHERE h.user.id = :userId
              AND h.status  = 'PENDING'
              AND h.dueDate < :today
            ORDER BY h.dueDate ASC
            """)
    List<Homework> findOverdue(
            @Param("userId") Long userId,
            @Param("today")  LocalDate today
    );

    // ── Đếm bài chưa nộp ─────────────────────────────────────────────────

    long countByUserIdAndStatus(Long userId, HomeworkStatus status);

    // ── Tổng xu từ nộp bài tập ───────────────────────────────────────────

    @Query("SELECT COALESCE(SUM(h.coinsEarned), 0) FROM Homework h WHERE h.user.id = :userId")
    int sumCoinsByUserId(@Param("userId") Long userId);
}
