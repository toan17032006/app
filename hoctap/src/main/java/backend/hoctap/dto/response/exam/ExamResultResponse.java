package backend.hoctap.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Trả về ngay sau khi học sinh nộp bài.
 * Lúc này mới được tiết lộ đáp án đúng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResultResponse {

    private Long attemptId;
    private String examTitle;

    /** Điểm số thang 10.0 */
    private BigDecimal score;

    private Integer correctCount;
    private Integer totalCount;

    /** Xu nhận được = correctCount × 15 */
    private Integer coinsEarned;

    /** COMPLETED | TIMED_OUT */
    private String status;

    private LocalDateTime submittedAt;

    /** Chi tiết từng câu đúng/sai — dùng để hiển thị giải thích */
    private List<AnswerResultItem> answers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AnswerResultItem {

        private Long questionId;
        private String questionText;

        /** Index học sinh đã chọn (null nếu bỏ qua) */
        private Integer selectedIndex;

        /** ✅ Đáp án đúng — chỉ xuất hiện SAU khi nộp bài */
        private Integer correctIndex;

        private String explanation;
        private Boolean isCorrect;
    }
}
