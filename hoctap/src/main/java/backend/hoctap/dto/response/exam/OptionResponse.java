package backend.hoctap.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Đáp án trả về khi làm bài.
 * KHÔNG chứa isCorrect — bảo mật đáp án đúng.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OptionResponse {

    private Long id;

    /** 0=A, 1=B, 2=C, 3=D */
    private Integer optionIndex;

    private String content;
}
