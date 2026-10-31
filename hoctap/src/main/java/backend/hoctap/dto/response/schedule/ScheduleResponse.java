package backend.hoctap.dto.response.schedule;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ScheduleResponse {

    private Long id;

    /** MON, TUE, WED, THU, FRI, SAT, SUN */
    private String dayOfWeek;

    private String subject;
    private String teacher;

    /** Định dạng HH:mm */
    private String startTime;

    /** Định dạng HH:mm */
    private String endTime;

    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
