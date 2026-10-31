package backend.hoctap.dto.response.homework;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomeworkResponse {

    private Long id;
    private String subject;
    private String title;
    private LocalDate dueDate;

    /** PENDING | SUBMITTED */
    private String status;

    /** Tên file đã nộp (null nếu chưa nộp) */
    private String fileName;

    /** URL tải file (null nếu chưa nộp) */
    private String fileUrl;

    /** Xu thưởng khi nộp bài (+20) */
    private Integer coinsEarned;

    private LocalDateTime submittedAt;
    private LocalDateTime createdAt;
}
