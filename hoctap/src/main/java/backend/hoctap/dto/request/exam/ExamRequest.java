package backend.hoctap.dto.request.exam;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ExamRequest {

    @NotBlank(message = "Tiêu đề bài thi không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @Size(max = 50, message = "Danh mục tối đa 50 ký tự")
    private String category;

    @Size(max = 1000, message = "Mô tả tối đa 1000 ký tự")
    private String description;

    /** Thời gian làm bài (phút) */
    @NotNull(message = "Thời gian làm bài không được để trống")
    @Min(value = 1, message = "Thời gian làm bài tối thiểu 1 phút")
    @Max(value = 180, message = "Thời gian làm bài tối đa 180 phút")
    private Integer durationMinutes;

    /** Điểm đạu (thang 10) */
    @DecimalMin(value = "0.0", message = "Điểm đạt tối thiểu là 0.0")
    @DecimalMax(value = "10.0", message = "Điểm đạt tối đa là 10.0")
    private Double passingScore;

    @NotNull(message = "Trạng thái không được để trống")
    private ExamStatus status;

    private LocalDate startTime;

    private LocalDate endTime;

    @NotEmpty(message = "Bài thi phải có ít nhất 1 câu hỏi")
    @Size(max = 100, message = "Bài thi tối đa 100 câu hỏi")
    @Valid
    private List<QuestionRequest> questions;

    // ── Nested DTOs ────────────────────────────────────────────────────────

    @Data
    public static class QuestionRequest {

        @NotBlank(message = "Nội dung câu hỏi không được để trống")
        @Size(max = 2000, message = "Nội dung câu hỏi tối đa 2000 ký tự")
        private String content;

        @NotNull(message = "Loại câu hỏi không được để trống")
        private QuestionType type;

        /**
         * Điểm cho câu hỏi này (tổng điểm các câu = 10).
         */
        @DecimalMin(value = "0.05", message = "Điểm câu hỏi tối thiểu 0.05")
        @DecimalMax(value = "10.0", message = "Điểm câu hỏi tối đa 10.0")
        private Double point;

        /** Giải thích đáp án đúng (hiển thị sau khi nộp bài) */
        private String explanation;

        @NotEmpty(message = "Câu hỏi phải có ít nhất 1 đáp án")
        @Size(max = 4, message = "Tối đa 4 đáp án mỗi câu")
        @Valid
        private List<OptionRequest> options;

        @Data
        public static class OptionRequest {

            @NotBlank(message = "Nội dung đáp án không được để trống")
            @Size(max = 500, message = "Nội dung đáp án tối đa 500 ký tự")
            private String content;

            @NotNull(message = "Trạng thái đúng/sai không được để trống")
            private Boolean isCorrect;
        }
    }

    // ── Enums ──────────────────────────────────────────────────────────────

    public enum QuestionType {
        SINGLE_CHOICE,   // Trắc nghiệm 1 đáp án
        MULTIPLE_CHOICE, // Trắc nghiệm nhiều đáp án
        TRUE_FALSE,      // Đúng/Sai
        ESSAY            // Tự luận
    }

    public enum ExamStatus {
        DRAFT,     // Nháp — chưa hiển thị cho học sinh
        PUBLISHED, // Đã công bố
        ARCHIVED   // Lưu trữ
    }
}
