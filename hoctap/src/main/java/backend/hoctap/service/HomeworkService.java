package backend.hoctap.service;

import backend.hoctap.dto.request.homework.HomeworkRequest;
import backend.hoctap.dto.response.homework.HomeworkResponse;
import backend.hoctap.entity.Homework;
import backend.hoctap.entity.Homework.HomeworkStatus;
import backend.hoctap.entity.User;
import backend.hoctap.repository.HomeworkRepository;
import backend.hoctap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    HOMEWORK SERVICE                         ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Nghiệp vụ quan trọng ở đây: khi học sinh nộp bài → +20 xu.
 *
 * Lưu ý về @Transactional và "chuỗi gọi hàm":
 * Khi homeworkService.submitHomework() gọi userService.addCoins():
 *   → Nếu addCoins() thất bại → submitHomework() cũng rollback.
 *   → Đảm bảo không bao giờ xảy ra tình trạng: bài đã nộp nhưng không được xu.
 */
@Service
@RequiredArgsConstructor
public class HomeworkService {

    private final HomeworkRepository homeworkRepository;
    private final UserRepository     userRepository;

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getAllByUser(Long userId) {
        return homeworkRepository
                .findByUserIdOrderByDueDateAsc(userId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<HomeworkResponse> getByStatus(Long userId, String statusStr) {
        HomeworkStatus status = parseStatus(statusStr);
        return homeworkRepository
                .findByUserIdAndStatus(userId, status)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Bài tập sắp đến hạn trong N ngày tới (chưa nộp) */
    @Transactional(readOnly = true)
    public List<HomeworkResponse> getUpcomingDue(Long userId, int days) {
        LocalDate deadline = LocalDate.now().plusDays(days);
        return homeworkRepository
                .findUpcomingDue(userId, deadline)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Bài tập đã quá hạn nhưng chưa nộp */
    @Transactional(readOnly = true)
    public List<HomeworkResponse> getOverdue(Long userId) {
        return homeworkRepository
                .findOverdue(userId, LocalDate.now())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE (Admin/Giáo viên tạo bài tập cho học sinh)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public HomeworkResponse create(Long userId, HomeworkRequest req) {
        User user = findUserOrThrow(userId);

        Homework homework = Homework.builder()
                .user(user)
                .subject(req.getSubject())
                .title(req.getTitle())
                .dueDate(req.getDueDate())
                .status(HomeworkStatus.PENDING)
                .build();

        return toResponse(homeworkRepository.save(homework));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT — Nghiệp vụ chính: nộp bài + thưởng xu
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Học sinh nộp bài tập.
     *
     * Có 2 cách gửi file:
     *   1. multipart file (ưu tiên) → lưu vào thư mục app.upload.dir, trả về
     *      fileUrl dạng /uploads/{tên-file-đã-đổi-để-an-toàn}
     *   2. fileName + fileUrl truyền sẵn (client tự có link)
     *
     * Luồng xử lý:
     *   1. Tìm homework, kiểm tra quyền
     *   2. Cập nhật trạng thái → SUBMITTED
     *   3. Cộng +20 xu vào tài khoản user
     *   4. Nếu bước 3 lỗi → bước 2 cũng rollback (nhờ @Transactional)
     */
    @Transactional
    public HomeworkResponse submitHomework(Long userId, Long homeworkId,
                                           org.springframework.web.multipart.MultipartFile file,
                                           String fileName, String fileUrl) {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy bài tập"));

        // Kiểm tra quyền sở hữu
        if (!homework.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền nộp bài tập này");
        }

        // Lưu file upload (nếu có) → fileUrl công khai qua /uploads/**
        if (file != null && !file.isEmpty()) {
            String originalName = file.getOriginalFilename();
            if (originalName != null && !originalName.isBlank()) {
                String storedName = storeFile(file);
                homework.setFileName(originalName);
                homework.setFileUrl("/uploads/" + storedName);
            }
        } else if (fileName != null || fileUrl != null) {
            // Client gửi sẵn link (nộp lại không kèm file mới → giữ nguyên link cũ)
            if (fileName != null) homework.setFileName(fileName);
            if (fileUrl != null) homework.setFileUrl(fileUrl);
        }

        // Cập nhật thông tin nộp bài
        homework.setStatus(HomeworkStatus.SUBMITTED);
        homework.setSubmittedAt(LocalDateTime.now());
        homework.setCoinsEarned(20);

        // Cộng xu trực tiếp vào user (dùng @Modifying query trong repository)
        // addCoins nằm trong cùng @Transactional → nếu lỗi sẽ rollback cả 2
        int updated = userRepository.addCoins(userId, 20);
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lỗi khi cộng xu");
        }

        return toResponse(homework);
    }

    /**
     * Lưu file upload vào thư mục app.upload.dir.
     * Tên file được đổi thành "hw-thờiGian-uuid-tênGốc" (đã sanitize)
     * để tránh trùng lặp và chống path traversal.
     */
    @org.springframework.beans.factory.annotation.Value("${app.upload.dir:./uploads}")
    private String uploadDir;

    private String storeFile(org.springframework.web.multipart.MultipartFile file) {
        try {
            java.nio.file.Path dir = java.nio.file.Paths.get(uploadDir).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(dir);

            // Chỉ giữ tên + phần mở rộng, loại bỏ mọi dấu / \ .. (chống path traversal)
            String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
            String safeName = java.nio.file.Paths.get(original).getFileName().toString()
                    .replaceAll("[^a-zA-Z0-9._\\-() ]", "_");

            String storedName = "hw-" + System.currentTimeMillis()
                    + "-" + java.util.UUID.randomUUID().toString().substring(0, 6)
                    + "-" + safeName;

            java.nio.file.Path target = dir.resolve(storedName).normalize();
            if (!target.startsWith(dir)) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Tên file không hợp lệ");
            }

            file.transferTo(target.toFile());
            return storedName;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Không thể lưu file: " + e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long userId, Long homeworkId) {
        Homework homework = homeworkRepository.findById(homeworkId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy bài tập"));

        if (!homework.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xóa bài tập này");
        }

        homeworkRepository.delete(homework);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private HomeworkResponse toResponse(Homework hw) {
        return HomeworkResponse.builder()
                .id(hw.getId())
                .subject(hw.getSubject())
                .title(hw.getTitle())
                .dueDate(hw.getDueDate())
                .status(hw.getStatus().name())
                .fileName(hw.getFileName())
                .fileUrl(hw.getFileUrl())
                .coinsEarned(hw.getCoinsEarned())
                .submittedAt(hw.getSubmittedAt())
                .createdAt(hw.getCreatedAt())
                .build();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private HomeworkStatus parseStatus(String s) {
        try {
            return HomeworkStatus.valueOf(s.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Trạng thái không hợp lệ. Giá trị: PENDING, SUBMITTED");
        }
    }
}
