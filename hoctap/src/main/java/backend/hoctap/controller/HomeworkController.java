package backend.hoctap.controller;

import backend.hoctap.dto.request.homework.HomeworkRequest;
import backend.hoctap.dto.response.common.ApiResponse;
import backend.hoctap.dto.response.homework.HomeworkResponse;
import backend.hoctap.service.HomeworkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * GET    /api/homework?userId=1              → Tất cả bài tập
 * GET    /api/homework?userId=1&status=PENDING → Lọc theo trạng thái
 * GET    /api/homework/upcoming?userId=1&days=3 → Sắp hết hạn
 * GET    /api/homework/overdue?userId=1        → Đã quá hạn
 * POST   /api/homework                         → Tạo bài tập (Admin)
 * POST   /api/homework/{id}/submit?userId=1    → Nộp bài
 * DELETE /api/homework/{id}?userId=1           → Xóa
 */
@RestController
@RequestMapping("/api/homework")
@RequiredArgsConstructor
public class HomeworkController {

    private final HomeworkService homeworkService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<HomeworkResponse>>> getHomework(
            @RequestParam Long userId,
            @RequestParam(required = false) String status) {

        List<HomeworkResponse> data = (status != null)
                ? homeworkService.getByStatus(userId, status)
                : homeworkService.getAllByUser(userId);

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/homework/upcoming?userId=1&days=3
     * Bài sắp hết hạn trong 3 ngày tới.
     */
    @GetMapping("/upcoming")
    public ResponseEntity<ApiResponse<List<HomeworkResponse>>> getUpcoming(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "3") int days) {

        return ResponseEntity.ok(
                ApiResponse.success(homeworkService.getUpcomingDue(userId, days)));
    }

    /** GET /api/homework/overdue?userId=1 */
    @GetMapping("/overdue")
    public ResponseEntity<ApiResponse<List<HomeworkResponse>>> getOverdue(
            @RequestParam Long userId) {

        return ResponseEntity.ok(
                ApiResponse.success(homeworkService.getOverdue(userId)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<HomeworkResponse>> create(
            @RequestParam Long userId,
            @Valid @RequestBody HomeworkRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bài tập thành công",
                        homeworkService.create(userId, req)));
    }

    /**
     * POST /api/homework/5/submit?userId=1
     *
     * Nộp bài với file đính kèm (multipart/form-data):
     *   - file:      file bài làm (PDF/ảnh/docx...) — tùy chọn, tối đa 5MB
     *   - fileName/fileUrl: nếu client đã có sẵn link file (không gửi file mới)
     *
     * File được lưu vào thư mục app.upload.dir (mặc định ./uploads)
     * và truy cập công khai qua /uploads/{tên-file}.
     */
    @PostMapping(value = "/{id}/submit", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<HomeworkResponse>> submitWithFile(
            @PathVariable Long id,
            @RequestParam Long userId,
            @RequestParam(required = false) org.springframework.web.multipart.MultipartFile file,
            @RequestParam(required = false) String fileName,
            @RequestParam(required = false) String fileUrl) {

        HomeworkResponse result = homeworkService.submitHomework(
                userId, id, file, fileName, fileUrl);

        return ResponseEntity.ok(
                ApiResponse.success("Nộp bài thành công! +20 xu", result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam Long userId) {

        homeworkService.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
