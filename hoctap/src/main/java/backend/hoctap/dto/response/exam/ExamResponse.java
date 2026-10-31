package backend.hoctap.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamResponse {

    private Long id;
    private String title;
    private String description;
    private String category;

    /** Thời gian làm bài (phút) — frontend dùng để hiển thị countdown */
    private Integer durationMinutes;

    private Integer questionsCount;
    private Double passingScore;

    /** DRAFT | PUBLISHED | ARCHIVED */
    private String status;

    private LocalDateTime createdAt;
}
