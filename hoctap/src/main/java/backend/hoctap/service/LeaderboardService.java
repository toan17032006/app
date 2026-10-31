package backend.hoctap.service;

import backend.hoctap.dto.response.leaderboard.LeaderboardResponse;
import backend.hoctap.entity.User;
import backend.hoctap.repository.ExamAttemptRepository;
import backend.hoctap.repository.HomeworkRepository;
import backend.hoctap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    LEADERBOARD SERVICE                      ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * @Service → Đánh dấu class này là một Spring Bean thuộc tầng Service.
 * Spring sẽ tự động tạo một instance và quản lý nó (gọi là IoC Container).
 *
 * @RequiredArgsConstructor (Lombok) → Tự động tạo constructor inject
 * tất cả field "final". Đây là cách inject phụ thuộc được khuyến nghị
 * thay vì dùng @Autowired trực tiếp lên field.
 */
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    // "final" + @RequiredArgsConstructor → Spring tự inject UserRepository vào đây
    private final UserRepository userRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final HomeworkRepository homeworkRepository;

    /**
     * Xác định mốc thời gian bắt đầu của kỳ xếp hạng:
     *   "week"  → 7 ngày gần nhất
     *   "month" → từ ngày đầu tiên của tháng hiện tại
     *   "all"/null → null (tính toàn bộ, không lọc thời gian)
     */
    public LocalDateTime resolvePeriodStart(String period) {
        if (period == null || period.isBlank() || "all".equalsIgnoreCase(period)) {
            return null;
        }
        switch (period.toLowerCase()) {
            case "week"  -> { return LocalDateTime.now().minusDays(7); }
            case "month" -> { return LocalDate.now().withDayOfMonth(1).atStartOfDay(); }
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Kỳ xếp hạng không hợp lệ. Giá trị: all, week, month");
        }
    }

    /**
     * ┌─────────────────────────────────────────────────────────┐
     * │  Lấy top N học sinh trên bảng xếp hạng                 │
     * └─────────────────────────────────────────────────────────┘
     *
     * @Transactional(readOnly = true) → Báo cho JPA biết đây là
     * giao dịch chỉ đọc. Lợi ích:
     *   ✅ Tối ưu hiệu năng (không cần flush/dirty-check)
     *   ✅ Database có thể dùng read replica
     *   ✅ Tránh vô tình ghi dữ liệu
     */
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getTopLeaderboard(int limit) {
        // Lấy danh sách user sắp xếp theo coins DESC, gpa DESC
        List<User> users = userRepository.findTopLeaderboard(limit);

        // Chuyển đổi từ Entity sang DTO (gọi là "mapping")
        // Entity chứa toàn bộ dữ liệu DB, DTO chỉ chứa những gì client cần
        List<LeaderboardResponse> result = new ArrayList<>();
        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            result.add(LeaderboardResponse.builder()
                    .rank(i + 1)               // Thứ hạng bắt đầu từ 1
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .grade(user.getGrade())
                    .avatarUrl(user.getAvatarUrl())
                    .gpa(user.getGpa())
                    .coins(user.getCoins())
                    .isCurrentUser(false)       // Controller sẽ set lại field này
                    .build());
        }
        return result;
    }

    /**
     * ┌─────────────────────────────────────────────────────────┐
     * │  Lấy toàn bộ bảng xếp hạng + đánh dấu user hiện tại   │
     * └─────────────────────────────────────────────────────────┘
     *
     * @param currentUserId ID của user đang đăng nhập (từ JWT token)
     */
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getFullLeaderboard(Long currentUserId) {
        List<User> users = userRepository.findLeaderboard();
        List<LeaderboardResponse> result = new ArrayList<>();

        for (int i = 0; i < users.size(); i++) {
            User user = users.get(i);
            result.add(LeaderboardResponse.builder()
                    .rank(i + 1)
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .grade(user.getGrade())
                    .avatarUrl(user.getAvatarUrl())
                    .gpa(user.getGpa())
                    .coins(user.getCoins())
                    // isCurrentUser = true nếu đây chính là người đang xem
                    .isCurrentUser(user.getId().equals(currentUserId))
                    .build());
        }
        return result;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // BẢNG XẾP HẠNG THEO KỲ (TUẦN / THÁNG)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Bảng xếp hạng theo kỳ: xếp hạng theo số xu KIẾM ĐƯỢC trong kỳ
     * (bài thi đã nộp + bài tập đã nộp kể từ mốc thời gian của kỳ).
     * Với period = "all" → dùng bảng xếp hạng toàn bộ như cũ.
     */
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getTopLeaderboard(int limit, String period) {
        LocalDateTime start = resolvePeriodStart(period);
        if (start == null) return getTopLeaderboard(limit);
        return buildPeriodBoard(null, start, limit);
    }

    /** Toàn bộ bảng xếp hạng theo kỳ, đánh dấu user đang xem (isCurrentUser) */
    @Transactional(readOnly = true)
    public List<LeaderboardResponse> getFullLeaderboard(Long currentUserId, String period) {
        LocalDateTime start = resolvePeriodStart(period);
        if (start == null) return getFullLeaderboard(currentUserId);
        return buildPeriodBoard(currentUserId, start, null);
    }

    /**
     * Xây bảng xếp hạng theo kỳ:
     *   1. Query tổng xu từ exam_attempts + homework (submitted_at >= start), group theo user
     *   2. Sắp xếp: xu kiếm được trong kỳ giảm dần → hòa thì so xu tích lũy
     *   3. Trường `coins` trong response = xu kiếm được TRONG KỲ; GPA vẫn là điểm tích lũy
     */
    private List<LeaderboardResponse> buildPeriodBoard(Long currentUserId,
                                                       LocalDateTime start,
                                                       Integer limit) {
        Map<Long, Integer> earnedInPeriod = new HashMap<>();
        for (Object[] row : examAttemptRepository.sumCoinsEarnedSince(
                start, backend.hoctap.entity.ExamAttempt.AttemptStatus.IN_PROGRESS)) {
            earnedInPeriod.put((Long) row[0], ((Number) row[1]).intValue());
        }
        for (Object[] row : homeworkRepository.sumHomeworkCoinsSince(start)) {
            earnedInPeriod.merge((Long) row[0], ((Number) row[1]).intValue(), Integer::sum);
        }

        List<User> users = new ArrayList<>(userRepository.findLeaderboard());
        users.sort(Comparator
                .comparingInt((User u) -> earnedInPeriod.getOrDefault(u.getId(), 0)).reversed()
                .thenComparing(Comparator.comparingInt(User::getCoins).reversed()));

        List<LeaderboardResponse> result = new ArrayList<>();
        int rank = 1;
        for (User user : users) {
            if (limit != null && rank > limit) break;
            result.add(LeaderboardResponse.builder()
                    .rank(rank++)
                    .userId(user.getId())
                    .fullName(user.getFullName())
                    .grade(user.getGrade())
                    .avatarUrl(user.getAvatarUrl())
                    .gpa(user.getGpa())
                    .coins(earnedInPeriod.getOrDefault(user.getId(), 0))
                    .isCurrentUser(user.getId().equals(currentUserId))
                    .build());
        }
        return result;
    }

    /**
     * ┌─────────────────────────────────────────────────────────┐
     * │  Lấy hạng hiện tại của một user cụ thể                 │
     * └─────────────────────────────────────────────────────────┘
     */
    @Transactional(readOnly = true)
    public int getUserRank(Long userId) {
        List<User> users = userRepository.findLeaderboard();
        for (int i = 0; i < users.size(); i++) {
            if (users.get(i).getId().equals(userId)) {
                return i + 1; // Trả về thứ hạng 1-based
            }
        }
        return -1; // Không tìm thấy
    }
}
