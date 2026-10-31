package backend.hoctap.dto.request.schedule;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ScheduleRequest {

    /**
     * Giá trị hợp lệ: MON, TUE, WED, THU, FRI, SAT, SUN
     */
    @NotBlank(message = "Ngày học không được để trống")
    private String dayOfWeek;

    @NotBlank(message = "Tên môn học không được để trống")
    @Size(max = 100, message = "Tên môn học tối đa 100 ký tự")
    private String subject;

    @NotBlank(message = "Giáo viên / phòng học không được để trống")
    @Size(max = 100, message = "Giáo viên tối đa 100 ký tự")
    private String teacher;

    /**
     * Định dạng HH:mm (ví dụ: 07:30)
     */
    @NotNull(message = "Giờ bắt đầu không được để trống")
    private String startTime;

    /**
     * Định dạng HH:mm (ví dụ: 09:00)
     */
    @NotNull(message = "Giờ kết thúc không được để trống")
    private String endTime;

    private String note;
}
