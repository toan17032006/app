package backend.hoctap.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStatsResponse {

    private BigDecimal gpa;
    private Integer coins;

    /** Số bài thi đã hoàn thành */
    private Long completedExams;

    /** Tổng số bài thi trong hệ thống */
    private Long totalExams;

    /** Số bài tập chưa nộp */
    private Long pendingHomework;

    /** Tổng bài tập được giao */
    private Long totalHomework;
}
