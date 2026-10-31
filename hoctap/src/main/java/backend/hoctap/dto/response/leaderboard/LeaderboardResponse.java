package backend.hoctap.dto.response.leaderboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/** Một hàng trong bảng xếp hạng */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LeaderboardResponse {

    /** Thứ hạng 1-based */
    private Integer rank;

    private Long userId;
    private String fullName;
    private String grade;
    private String avatarUrl;
    private BigDecimal gpa;
    private Integer coins;

    /** true nếu đây là người đang đăng nhập xem bảng xếp hạng */
    private Boolean isCurrentUser;
}
