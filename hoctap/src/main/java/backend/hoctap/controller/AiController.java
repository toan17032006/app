package backend.hoctap.controller;

import backend.hoctap.dto.response.ai.DetectCodesResponse;
import backend.hoctap.dto.response.common.ApiResponse;
import backend.hoctap.dto.response.exam.ExamResponse;
import backend.hoctap.service.ai.AiExamService;
import backend.hoctap.service.ai.FileTextExtractor;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║              AI AGENT CONTROLLER — tạo đề từ file            ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * POST /api/ai/detect-exam-codes  (multipart/form-data, cần JWT)
 *   → Đọc tài liệu, liệt kê các MÃ ĐỀ tìm được (vd đề THPT in 101/102/103).
 *     Frontend hiện danh sách cho người dùng chọn trước khi giao đề.
 *
 * POST /api/ai/generate-exam  (multipart/form-data, cần JWT)
 *   file             : file tài liệu (PDF, DOC, DOCX, TXT) — tối đa 15MB
 *   mode             : EXAM (tạo phòng thi) | HOMEWORK (bài tập có giải thích)
 *   durationMinutes  : thời gian nếu người dùng tự chọn (bỏ trống = tự theo tài liệu)
 *   questionCount    : số câu hỏi muốn tạo (mặc định 10, tối đa 100)
 *   grade            : lớp của người học (Năm nhất...Năm tư, 10A1...12A3) — điều chỉnh độ khó
 *   examCode         : mã đề được chọn (từ detect-exam-codes) — AI CHỈ lấy câu
 *                      hỏi của mã đề này, bỏ qua các mã khác trong tài liệu
 *
 * AI đọc tài liệu → sinh câu hỏi trắc nghiệm + đáp án → tạo Exam thật trong DB
 * (chấm tự động bằng luồng nộp bài hiện có).
 */
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiExamService aiExamService;
    private final FileTextExtractor fileTextExtractor;

    /** Đọc tài liệu và liệt kê các mã đề thi tìm được (regex + AI dự phòng) */
    @PostMapping(value = "/detect-exam-codes", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<DetectCodesResponse>> detectExamCodes(
            @RequestParam MultipartFile file) {

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Vui lòng chọn file tài liệu"));
        }
        String documentText;
        try {
            documentText = fileTextExtractor.extract(
                    file.getOriginalFilename(), file.getBytes());
        } catch (java.io.IOException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("Không đọc được nội dung file"));
        }

        return ResponseEntity.ok(ApiResponse.success(
                aiExamService.detectCodes(documentText)));
    }

    @PostMapping(value = "/generate-exam", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<ExamResponse>> generateExam(
            @RequestParam MultipartFile file,
            @RequestParam(defaultValue = "EXAM") String mode,
            @RequestParam(required = false) Integer durationMinutes,
            @RequestParam(required = false) Integer questionCount,
            @RequestParam(required = false) String grade,
            @RequestParam(required = false) String examCode) {

        ExamResponse exam = aiExamService.generateFromFile(
                file, mode, durationMinutes, questionCount, grade, examCode);

        String message = "HOMEWORK".equalsIgnoreCase(mode)
                ? String.format("Đã tạo bài tập \"%s\" với %d câu hỏi có giải thích",
                        exam.getTitle(), exam.getQuestionsCount())
                : String.format("Đã tạo đề thi \"%s\" với %d câu hỏi — thời gian làm bài %d phút",
                        exam.getTitle(), exam.getQuestionsCount(),
                        exam.getDurationMinutes() == null ? 0 : exam.getDurationMinutes());

        return ResponseEntity.ok(ApiResponse.success(message, exam));
    }
}

