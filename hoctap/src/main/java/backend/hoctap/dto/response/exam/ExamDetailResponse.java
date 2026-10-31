package backend.hoctap.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Trả về khi học sinh bắt đầu làm bài.
 * Có câu hỏi + đáp án, KHÔNG có correctOptionIndex.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamDetailResponse {

    private Long examId;

    /** ID lần làm bài vừa được tạo — dùng khi nộp bài */
    private Long attemptId;

    private String title;
    private String category;
    private Integer durationMinutes;
    private List<QuestionResponse> questions;
}
