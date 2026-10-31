package backend.hoctap.service;

import backend.hoctap.dto.request.user.ChangePasswordRequest;
import backend.hoctap.dto.request.user.UpdateProfileRequest;
import backend.hoctap.dto.response.user.UserResponse;
import backend.hoctap.dto.response.user.UserStatsResponse;
import backend.hoctap.entity.User;
import backend.hoctap.repository.ExamAttemptRepository;
import backend.hoctap.repository.ExamRepository;
import backend.hoctap.repository.HomeworkRepository;
import backend.hoctap.repository.UserRepository;
import backend.hoctap.entity.ExamAttempt.AttemptStatus;
import backend.hoctap.entity.Homework.HomeworkStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    USER SERVICE                             ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Service trung tâm — các service khác (Homework, Exam) gọi
 * addCoins() ở đây thay vì gọi thẳng Repository.
 *
 * PasswordEncoder → Interface của Spring Security dùng để:
 *   - encode(rawPassword)       : Hash mật khẩu thô thành chuỗi BCrypt
 *   - matches(raw, encoded)     : So sánh mật khẩu thô với hash trong DB
 *   KHÔNG BAO GIỜ lưu plain text vào database!
 */
@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository        userRepository;
    private final ExamRepository        examRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final HomeworkRepository    homeworkRepository;
    private final PasswordEncoder       passwordEncoder;  // inject từ SecurityConfig

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserResponse getProfile(Long userId) {
        User user = findOrThrow(userId);
        return toResponse(user);
    }

    /**
     * Lấy thống kê tổng quan cho Dashboard.
     * Phải gọi nhiều repository → tổng hợp vào 1 DTO.
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getStats(Long userId) {
        User user = findOrThrow(userId);

        long completedExams = examAttemptRepository
                .countByUserIdAndStatus(userId, AttemptStatus.COMPLETED);

        long totalExams = examRepository.count(); // đếm tổng bài thi trong hệ thống

        long pendingHomework = homeworkRepository
                .countByUserIdAndStatus(userId, HomeworkStatus.PENDING);

        long totalHomework = homeworkRepository
                .findByUserIdOrderByDueDateAsc(userId).size();

        return UserStatsResponse.builder()
                .gpa(user.getGpa())
                .coins(user.getCoins())
                .completedExams(completedExams)
                .totalExams(totalExams)
                .pendingHomework(pendingHomework)
                .totalHomework(totalHomework)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public UserResponse updateProfile(Long userId, UpdateProfileRequest req) {
        User user = findOrThrow(userId);
        user.setFullName(req.getFullName());
        user.setGrade(req.getGrade());
        user.setAvatarUrl(req.getAvatarUrl());
        // JPA dirty checking: không cần gọi save() vì user đã được quản lý (managed entity)
        // Khi @Transactional kết thúc → JPA tự động UPDATE xuống DB
        return toResponse(user);
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        User user = findOrThrow(userId);

        // Kiểm tra mật khẩu hiện tại có đúng không
        if (!passwordEncoder.matches(req.getCurrentPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mật khẩu hiện tại không đúng");
        }

        // Kiểm tra confirm password
        if (!req.getNewPassword().equals(req.getConfirmPassword())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Xác nhận mật khẩu không khớp");
        }

        // Hash mật khẩu mới trước khi lưu
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // COINS (được gọi bởi ExamService và HomeworkService)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Cộng xu vào tài khoản user.
     *
     * Tại sao tách riêng method này thay vì dùng thẳng userRepository.addCoins()?
     * → Để sau này có thể thêm logic: ghi log, gửi notification, giới hạn xu...
     *   mà không cần sửa nhiều chỗ.
     */
    @Transactional
    public void addCoins(Long userId, int amount) {
        if (amount <= 0) return;
        int updated = userRepository.addCoins(userId, amount);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Không tìm thấy người dùng để cộng xu");
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void deleteAccount(Long userId) {
        User user = findOrThrow(userId);
        // CascadeType.ALL trên User entity → tự xóa schedules, attempts, homework
        userRepository.delete(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    public User findOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    public UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .grade(user.getGrade())
                .gpa(user.getGpa())
                .coins(user.getCoins())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider().name())
                .createdAt(user.getCreatedAt())
                .build();
        // Lưu ý: passwordHash và googleId KHÔNG được map vào DTO
    }
}
