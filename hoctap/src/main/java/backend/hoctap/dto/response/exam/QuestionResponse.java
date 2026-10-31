package backend.hoctap.dto.response.exam;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Câu hỏi trả về khi làm bài.
 * KHÔNG chứa correctOptionIndex — bảo mật đáp án.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuestionResponse {

    private Long id;
    private String content;
    private String type;       // SINGLE_CHOICE | MULTIPLE_CHOICE | TRUE_FALSE | ESSAY
    private Integer orderIndex;
    private List<OptionResponse> options;
}
