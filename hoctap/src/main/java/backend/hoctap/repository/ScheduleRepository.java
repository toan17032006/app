package backend.hoctap.repository;

import backend.hoctap.entity.Schedule;
import backend.hoctap.entity.Schedule.DayOfWeek;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    // ── Lấy toàn bộ lịch theo user ────────────────────────────────────────

    List<Schedule> findByUserIdOrderByDayOfWeekAscStartTimeAsc(Long userId);

    // ── Lọc theo ngày trong tuần ───────────────────────────────────────────

    List<Schedule> findByUserIdAndDayOfWeek(Long userId, DayOfWeek dayOfWeek);

    // ── Kiểm tra xung đột giờ học ──────────────────────────────────────────

    /**
     * Tìm lịch trùng giờ (để kiểm tra conflict khi thêm/sửa).
     * Bỏ qua chính schedule đang sửa nếu có excludeId.
     */
    @Query("""
            SELECT s FROM Schedule s
            WHERE s.user.id = :userId
              AND s.dayOfWeek = :day
              AND s.id <> :excludeId
              AND s.startTime < :endTime
              AND s.endTime   > :startTime
            """)
    List<Schedule> findConflicting(
            @Param("userId")    Long userId,
            @Param("day")       DayOfWeek day,
            @Param("startTime") java.time.LocalTime startTime,
            @Param("endTime")   java.time.LocalTime endTime,
            @Param("excludeId") Long excludeId
    );

    // ── Xóa toàn bộ lịch của một user ─────────────────────────────────────

    void deleteByUserId(Long userId);
}
