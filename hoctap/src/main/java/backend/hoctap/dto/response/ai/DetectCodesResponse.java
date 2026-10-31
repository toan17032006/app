package backend.hoctap.dto.response.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Kết quả dò mã đề thi trong tài liệu (trước khi AI giao đề).
 *
 * Một tài liệu có thể chứa NHIỀU mã đề (vd đề thi THPT in 101, 102, 103...).
 * Frontend hiển thị danh sách để người dùng chọn, rồi gọi generate-exam kèm
 * examCode đã chọn.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DetectCodesResponse {

    /** Danh sách mã đề tìm được, vd ["101", "102", "103"] */
    private List<String> codes;

    /** Nguồn dò: "regex" (quét trực tiếp trong text) hoặc "ai" (AI đọc) */
    private String source;

    /** Thời gian làm bài tìm thấy trong tài liệu (phút) — null nếu không ghi */
    private Integer durationMinutes;

    /** Số câu hỏi ghi trong tài liệu — null nếu không ghi */
    private Integer questionCount;
}
