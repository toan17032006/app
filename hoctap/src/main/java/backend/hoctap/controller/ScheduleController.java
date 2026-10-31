package backend.hoctap.controller;

import backend.hoctap.dto.request.schedule.ScheduleRequest;
import backend.hoctap.dto.response.common.ApiResponse;
import backend.hoctap.dto.response.schedule.ScheduleResponse;
import backend.hoctap.service.ScheduleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                  SCHEDULE CONTROLLER                        ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * REST API chuẩn — CRUD đầy đủ:
 *
 *   GET    /api/schedules          → Lấy tất cả
 *   GET    /api/schedules?day=MON  → Lọc theo ngày
 *   POST   /api/schedules          → Thêm mới
 *   PUT    /api/schedules/{id}     → Sửa
 *   DELETE /api/schedules/{id}     → Xóa
 *
 * @Valid → Kích hoạt validation từ các annotation trong DTO
 *   (@NotBlank, @Size, @Min, v.v.)
 *   Nếu request không hợp lệ → Spring tự động trả 400 Bad Request
 *   mà không cần viết thêm code kiểm tra.
 *
 * TODO: Sau khi có Spring Security, lấy userId từ JWT token
 *       thay vì @RequestParam như hiện tại.
 *   Ví dụ: @AuthenticationPrincipal UserDetails user
 */
@RestController
@RequestMapping("/api/schedules")
@RequiredArgsConstructor
public class ScheduleController {

    private final ScheduleService scheduleService;

    /**
     * GET /api/schedules?userId=1
     * GET /api/schedules?userId=1&day=MON
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ScheduleResponse>>> getSchedules(
            @RequestParam Long userId,
            @RequestParam(required = false) String day) {

        List<ScheduleResponse> data = (day != null)
                ? scheduleService.getByDay(userId, day)
                : scheduleService.getAllByUser(userId);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * POST /api/schedules
     * Body: { "dayOfWeek": "MON", "subject": "Toán", ... }
     *
     * @RequestBody → Đọc JSON từ request body, convert thành Java object
     * @Valid       → Validate object vừa đọc được
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ScheduleResponse>> create(
            @RequestParam Long userId,
            @Valid @RequestBody ScheduleRequest req) {

        ScheduleResponse created = scheduleService.create(userId, req);

        // 201 Created thay vì 200 OK — đúng convention REST khi tạo mới
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Thêm lịch học thành công", created));
    }

    /**
     * PUT /api/schedules/3?userId=1
     * Body: { "dayOfWeek": "TUE", "subject": "Lý", ... }
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<ScheduleResponse>> update(
            @PathVariable Long id,
            @RequestParam Long userId,
            @Valid @RequestBody ScheduleRequest req) {

        ScheduleResponse updated = scheduleService.update(userId, id, req);
        return ResponseEntity.ok(ApiResponse.success("Cập nhật lịch học thành công", updated));
    }

    /**
     * DELETE /api/schedules/3?userId=1
     * Trả về 204 No Content — đúng convention REST khi xóa thành công
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam Long userId) {

        scheduleService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
