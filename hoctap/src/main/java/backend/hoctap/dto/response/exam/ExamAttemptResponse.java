package backend.hoctap.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Mỗi dòng trong lịch sử làm bài của học sinh */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamAttemptResponse {

    private Long id;
    private Long examId;
    private String examTitle;
    private String category;

    private BigDecimal score;
    private Integer correctCount;
    private Integer totalCount;
    private Integer coinsEarned;

    /** COMPLETED | TIMED_OUT | IN_PROGRESS */
    private String status;

    private LocalDateTime startedAt;
    private LocalDateTime submittedAt;
}
