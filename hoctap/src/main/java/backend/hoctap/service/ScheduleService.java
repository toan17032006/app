package backend.hoctap.service;

import backend.hoctap.dto.request.schedule.ScheduleRequest;
import backend.hoctap.dto.response.schedule.ScheduleResponse;
import backend.hoctap.entity.Schedule;
import backend.hoctap.entity.Schedule.DayOfWeek;
import backend.hoctap.entity.User;
import backend.hoctap.repository.ScheduleRepository;
import backend.hoctap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    SCHEDULE SERVICE                         ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Đây là Service quản lý thời gian biểu (CRUD đầy đủ).
 * Business logic chính: kiểm tra trùng giờ trước khi thêm/sửa.
 *
 * ResponseStatusException → Cách ném lỗi đơn giản trong Spring Boot.
 * Khi ném exception này, Spring tự động trả về HTTP status tương ứng.
 * Ví dụ: throw new ResponseStatusException(HttpStatus.NOT_FOUND, "...")
 * → Client nhận được response 404 Not Found.
 */
@Service
@RequiredArgsConstructor
public class ScheduleService {

    private final ScheduleRepository scheduleRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm");

    // ─────────────────────────────────────────────────────────────────────────
    // READ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy toàn bộ lịch học của một user.
     * readOnly = true vì chỉ đọc dữ liệu.
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getAllByUser(Long userId) {
        return scheduleRepository
                .findByUserIdOrderByDayOfWeekAscStartTimeAsc(userId)
                .stream()
                .map(this::toResponse)   // method reference — gọi hàm toResponse() cho từng item
                .toList();
    }

    /**
     * Lọc lịch học theo ngày trong tuần.
     *
     * Tại sao không để Controller tự lọc?
     * → Vì logic lọc là nghiệp vụ, thuộc về Service.
     *   Controller chỉ nhận và trả, không tự xử lý.
     */
    @Transactional(readOnly = true)
    public List<ScheduleResponse> getByDay(Long userId, String dayStr) {
        DayOfWeek day = parseDayOfWeek(dayStr);
        return scheduleRepository
                .findByUserIdAndDayOfWeek(userId, day)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Thêm tiết học mới.
     *
     * @Transactional (không có readOnly) → Giao dịch đọc + GHI.
     * Nếu bất kỳ bước nào ném Exception → toàn bộ thay đổi bị rollback.
     * Ví dụ: nếu save() thành công nhưng sau đó có lỗi khác,
     * DB sẽ tự động hoàn tác — không bị dữ liệu nửa vời.
     */
    @Transactional
    public ScheduleResponse create(Long userId, ScheduleRequest req) {
        User user = findUserOrThrow(userId);

        LocalTime start = LocalTime.parse(req.getStartTime(), TIME_FMT);
        LocalTime end   = LocalTime.parse(req.getEndTime(), TIME_FMT);

        // Validate: giờ bắt đầu phải trước giờ kết thúc
        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Giờ bắt đầu phải trước giờ kết thúc");
        }

        DayOfWeek day = parseDayOfWeek(req.getDayOfWeek());

        // Kiểm tra xung đột giờ học (dùng query có sẵn trong Repository)
        // excludeId = 0L vì đây là thêm mới, không có ID để loại trừ
        List<Schedule> conflicts = scheduleRepository.findConflicting(
                userId, day, start, end, 0L);

        if (!conflicts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khung giờ này đã có tiết học khác. Vui lòng chọn giờ khác.");
        }

        // Tạo Entity từ DTO rồi lưu vào DB
        Schedule schedule = Schedule.builder()
                .user(user)
                .dayOfWeek(day)
                .subject(req.getSubject())
                .teacher(req.getTeacher())
                .startTime(start)
                .endTime(end)
                .note(req.getNote())
                .build();

        // save() → INSERT SQL vào database, trả về entity đã có ID
        Schedule saved = scheduleRepository.save(schedule);
        return toResponse(saved);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // UPDATE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ScheduleResponse update(Long userId, Long scheduleId, ScheduleRequest req) {
        // Tìm schedule, nếu không tồn tại → 404
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy lịch học"));

        // Kiểm tra quyền sở hữu: user chỉ được sửa lịch của chính mình
        if (!schedule.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền sửa lịch này");
        }

        LocalTime start = LocalTime.parse(req.getStartTime(), TIME_FMT);
        LocalTime end   = LocalTime.parse(req.getEndTime(), TIME_FMT);

        if (!start.isBefore(end)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Giờ bắt đầu phải trước giờ kết thúc");
        }

        DayOfWeek day = parseDayOfWeek(req.getDayOfWeek());

        // Kiểm tra conflict, loại trừ chính lịch đang sửa (excludeId = scheduleId)
        List<Schedule> conflicts = scheduleRepository.findConflicting(
                userId, day, start, end, scheduleId);

        if (!conflicts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Khung giờ này đã có tiết học khác");
        }

        // Cập nhật field (JPA tự theo dõi thay đổi, cuối @Transactional sẽ UPDATE)
        schedule.setDayOfWeek(day);
        schedule.setSubject(req.getSubject());
        schedule.setTeacher(req.getTeacher());
        schedule.setStartTime(start);
        schedule.setEndTime(end);
        schedule.setNote(req.getNote());

        return toResponse(schedule);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // DELETE
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public void delete(Long userId, Long scheduleId) {
        Schedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy lịch học"));

        if (!schedule.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền xóa lịch này");
        }

        scheduleRepository.delete(schedule);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER (private — chỉ dùng trong class này)
    // ─────────────────────────────────────────────────────────────────────────

    /** Chuyển Entity Schedule → DTO ScheduleResponse */
    private ScheduleResponse toResponse(Schedule s) {
        return ScheduleResponse.builder()
                .id(s.getId())
                .dayOfWeek(s.getDayOfWeek().name())
                .subject(s.getSubject())
                .teacher(s.getTeacher())
                .startTime(s.getStartTime().format(TIME_FMT))
                .endTime(s.getEndTime().format(TIME_FMT))
                .note(s.getNote())
                .createdAt(s.getCreatedAt())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private User findUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy người dùng"));
    }

    private DayOfWeek parseDayOfWeek(String dayStr) {
        try {
            return DayOfWeek.valueOf(dayStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Ngày không hợp lệ. Giá trị: MON, TUE, WED, THU, FRI, SAT, SUN");
        }
    }
}
