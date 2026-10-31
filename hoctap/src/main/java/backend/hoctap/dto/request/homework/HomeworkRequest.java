package backend.hoctap.dto.request.homework;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class HomeworkRequest {

    @NotBlank(message = "Môn học không được để trống")
    @Size(max = 50, message = "Môn học tối đa 50 ký tự")
    private String subject;

    @NotBlank(message = "Tiêu đề bài tập không được để trống")
    @Size(max = 200, message = "Tiêu đề tối đa 200 ký tự")
    private String title;

    @NotNull(message = "Hạn chót không được để trống")
    @Future(message = "Hạn chót phải là ngày trong tương lai")
    private LocalDate dueDate;
}
