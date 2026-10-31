package backend.hoctap.controller;

import backend.hoctap.dto.request.exam.ExamRequest;
import backend.hoctap.dto.request.exam.SubmitExamRequest;
import backend.hoctap.dto.response.common.ApiResponse;
import backend.hoctap.dto.response.exam.*;
import backend.hoctap.service.ExamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    EXAM CONTROLLER                          ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Luồng làm bài:
 *   1. GET  /api/exams             → Xem danh sách bài thi
 *   2. POST /api/exams/{id}/start  → Bắt đầu làm bài
 *      ← Nhận về: attemptId + danh sách câu hỏi (KHÔNG có đáp án đúng)
 *   3. POST /api/exams/submit      → Nộp bài
 *      ← Nhận về: điểm số + đáp án đúng + xu thưởng
 *
 *   4. GET  /api/exams/history?userId=1 → Lịch sử làm bài
 *   5. POST /api/exams              → Tạo bài thi mới (Admin)
 */
@RestController
@RequestMapping("/api/exams")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    // ── READ ─────────────────────────────────────────────────────────────────

    @GetMapping
    public ResponseEntity<ApiResponse<List<ExamResponse>>> getAll(
            @RequestParam(required = false) String category) {

        List<ExamResponse> data = (category != null)
                ? examService.getByCategory(category)
                : examService.getAllExams();

        return ResponseEntity.ok(ApiResponse.success(data));
    }

    @GetMapping("/history")
    public ResponseEntity<ApiResponse<List<ExamAttemptResponse>>> getHistory(
            @RequestParam Long userId) {

        return ResponseEntity.ok(ApiResponse.success(examService.getHistory(userId)));
    }

    // ── START ────────────────────────────────────────────────────────────────

    /**
     * POST /api/exams/5/start?userId=1
     *
     * Không cần @RequestBody vì không có dữ liệu gửi lên.
     * Chỉ cần biết examId (từ URL) và userId (từ param).
     *
     * Trả về: toàn bộ câu hỏi + attemptId (dùng khi nộp bài).
     */
    @PostMapping("/{examId}/start")
    public ResponseEntity<ApiResponse<ExamDetailResponse>> startExam(
            @PathVariable Long examId,
            @RequestParam Long userId) {

        ExamDetailResponse detail = examService.startExam(userId, examId);
        return ResponseEntity.ok(
                ApiResponse.success("Bắt đầu làm bài thành công", detail));
    }

    // ── SUBMIT ───────────────────────────────────────────────────────────────

    /**
     * POST /api/exams/submit?userId=1
     * Body: {
     *   "attemptId": 3,
     *   "answers": [
     *     { "questionId": 1, "selectedOptionIndex": 2 },
     *     { "questionId": 2, "selectedOptionIndex": 0 }
     *   ]
     * }
     *
     * @RequestParam(defaultValue = "false") timedOut
     *   → Frontend gửi timedOut=true nếu đồng hồ đếm ngược hết giờ.
     */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<ExamResultResponse>> submit(
            @RequestParam Long userId,
            @RequestParam(defaultValue = "false") boolean timedOut,
            @Valid @RequestBody SubmitExamRequest req) {

        ExamResultResponse result = examService.submitExam(userId, req, timedOut);

        String msg = timedOut
                ? String.format("Hết giờ! Bạn đạt %.1f điểm và nhận %d xu",
                        result.getScore(), result.getCoinsEarned())
                : String.format("Nộp bài thành công! Bạn đạt %.1f điểm và nhận %d xu",
                        result.getScore(), result.getCoinsEarned());

        return ResponseEntity.ok(ApiResponse.success(msg, result));
    }

    // ── ADMIN: CREATE ─────────────────────────────────────────────────────────

    /**
     * POST /api/exams
     * Tạo bài thi mới (chỉ Admin mới được dùng — sau này thêm @PreAuthorize).
     */
    @PostMapping
    public ResponseEntity<ApiResponse<ExamResponse>> createExam(
            @Valid @RequestBody ExamRequest req) {

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo bài thi thành công",
                        examService.createExam(req)));
    }
}
