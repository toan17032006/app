package backend.hoctap.dto.request.exam;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitExamRequest {

    @NotNull(message = "ID lần làm bài không được để trống")
    private Long attemptId;

    @NotNull(message = "Danh sách câu trả lời không được để trống")
    private List<AnswerItem> answers;

    @Data
    public static class AnswerItem {

        @NotNull(message = "ID câu hỏi không được để trống")
        private Long questionId;

        /**
         * Index đáp án học sinh chọn (0=A, 1=B, 2=C, 3=D).
         * null nếu học sinh bỏ qua câu này.
         */
        private Integer selectedOptionIndex;
    }
}
