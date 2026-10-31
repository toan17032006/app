package backend.hoctap.config;

import backend.hoctap.entity.Exam;
import backend.hoctap.entity.Homework;
import backend.hoctap.entity.Option;
import backend.hoctap.entity.Question;
import backend.hoctap.entity.Schedule;
import backend.hoctap.entity.User;
import backend.hoctap.repository.ExamRepository;
import backend.hoctap.repository.HomeworkRepository;
import backend.hoctap.repository.ScheduleRepository;
import backend.hoctap.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/**
 * Tạo dữ liệu MẪU khi khởi động để app mới cài là có nội dung dùng thử:
 *   - 4 học sinh demo (có xu + GPA cho bảng xếp hạng)
 *   - 4 bài kiểm tra trắc nghiệm (Toán, Lý, Anh, Hóa) × 5 câu
 *   - Thời gian biểu + bài tập của học sinh demo đầu tiên
 *
 * CHỈ chạy khi database còn trống (chưa có user nào) và app.seed.enabled=true
 * (biến môi trường SEED_DATA=false để tắt khi deploy production).
 *
 * Tài khoản demo (đăng nhập thử ngay):
 *   hs1@edusmart.vn / HocSinh@123  ...  hs4@edusmart.vn / HocSinh@123
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    /** Một câu hỏi trắc nghiệm mẫu: nội dung + 4 lựa chọn + index đáp án đúng (0-3) */
    private record Q(String text, int correct, String... options) {}

    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final HomeworkRepository homeworkRepository;
    private final ScheduleRepository scheduleRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.enabled:true}")
    private boolean seedEnabled;

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) return;
        if (userRepository.count() > 0) return; // Đã có dữ liệu → không seed

        log.info(">>> Database trống — tạo dữ liệu mẫu cho EduSmart...");

        seedUsers();
        seedExams();
        seedScheduleAndHomework();

        log.info(">>> Đã tạo xong dữ liệu mẫu. Đăng nhập thử: hs1@edusmart.vn / HocSinh@123");
    }

    // ── Học sinh demo ────────────────────────────────────────────────────────
    private void seedUsers() {
        String password = passwordEncoder.encode("HocSinh@123");
        List<User> users = List.of(
                User.builder().fullName("Nguyễn Thị Mai").email("hs1@edusmart.vn")
                        .passwordHash(password).grade("12A1")
                        .gpa(new BigDecimal("3.80")).coins(320)
                        .avatarUrl(null).build(),
                User.builder().fullName("Trần Quốc Bảo").email("hs2@edusmart.vn")
                        .passwordHash(password).grade("12A2")
                        .gpa(new BigDecimal("3.65")).coins(280)
                        .avatarUrl(null).build(),
                User.builder().fullName("Lê Hải Yến").email("hs3@edusmart.vn")
                        .passwordHash(password).grade("11A1")
                        .gpa(new BigDecimal("3.50")).coins(210)
                        .avatarUrl(null).build(),
                User.builder().fullName("Phạm Minh Tuấn").email("hs4@edusmart.vn")
                        .passwordHash(password).grade("11A2")
                        .gpa(new BigDecimal("3.20")).coins(150)
                        .avatarUrl(null).build()
        );
        userRepository.saveAll(users);
    }

    // ── Bài kiểm tra mẫu: 4 môn × 5 câu ─────────────────────────────────────
    private void seedExams() {
        seedExam("Kiểm tra 15 phút — Hàm số & Phương trình", "Toán học", 15, List.of(
                new Q("Nghiệm của phương trình 2x + 4 = 0 là?", 1, "x = 2", "x = -2", "x = 4", "x = -4"),
                new Q("Đồ thị hàm số y = x² có dạng?", 1, "Đường thẳng", "Parabol", "Hypebol", "Đường tròn"),
                new Q("Cho f(x) = x + 3. Giá trị f(5) bằng?", 2, "5", "7", "8", "15"),
                new Q("Hệ phương trình: x + y = 3 và x - y = 1. Giá trị x là?", 1, "1", "2", "3", "4"),
                new Q("Tập xác định của hàm số y = √(x - 1) là?", 0, "x ≥ 1", "x > 1", "x ≤ 1", "Mọi x")
        ));

        seedExam("Kiểm tra 15 phút — Điện học", "Vật lý", 15, List.of(
                new Q("Đơn vị đo cường độ dòng điện là?", 1, "Vôn (V)", "Ampe (A)", "Ôm (Ω)", "Oat (W)"),
                new Q("Định luật Ohm: I = ?", 1, "U × R", "U / R", "R / U", "U + R"),
                new Q("Hai điện trở 2Ω và 3Ω nối tiếp. Điện trở tương đương?", 2, "1Ω", "1,2Ω", "5Ω", "6Ω"),
                new Q("Đơn vị đo công suất điện là?", 2, "Jun (J)", "Vôn (V)", "Oat (W)", "Coulomb (C)"),
                new Q("Dòng điện một chiều (DC) có đặc điểm gì?", 1, "Đổi chiều theo chu kỳ",
                        "Không đổi chiều", "Tần số 50Hz", "Chỉ có trong nhà máy")
        ));

        seedExam("Kiểm tra 15 phút — Thì hiện tại", "Tiếng Anh", 15, List.of(
                new Q("She ___ to school every day.", 1, "go", "goes", "going", "gone"),
                new Q("I ___ my homework at the moment.", 2, "do", "does", "am doing", "did"),
                new Q("They ___ football on Sundays.", 1, "plays", "play", "playing", "are play"),
                new Q("Đâu là câu hỏi đúng của 'You are a student'?", 0,
                        "Are you a student?", "Do you are a student?", "You are a student?", "Is you a student?"),
                new Q("He ___ like coffee.", 1, "don't", "doesn't", "isn't", "not")
        ));

        seedExam("Kiểm tra 15 phút — Bảng tuần hoàn", "Hóa học", 15, List.of(
                new Q("Ký hiệu hóa học của oxy là?", 0, "O", "Ox", "Oy", "Og"),
                new Q("Nguyên tử nào nhẹ nhất?", 1, "Heli", "Hidro", "Lithium", "Carbon"),
                new Q("Nước có công thức hóa học là?", 1, "HO", "H₂O", "H₂O₂", "OH₃"),
                new Q("NaCl là tên gọi của?", 1, "Đường", "Muối ăn", "Nước chanh", "Baking soda"),
                new Q("Số hiệu nguyên tử của Carbon là?", 2, "4", "5", "6", "12")
        ));
    }

    private void seedExam(String title, String category, int minutes, List<Q> questionDefs) {
        Exam exam = Exam.builder()
                .title(title)
                .category(category)
                .durationMinutes(minutes)
                .questionsCount(questionDefs.size())
                .build();

        for (int q = 0; q < questionDefs.size(); q++) {
            Q def = questionDefs.get(q);
            Question question = Question.builder()
                    .exam(exam)
                    .questionText(def.text())
                    .correctOptionIndex(def.correct())
                    .orderIndex(q + 1)
                    .build();
            for (int o = 0; o < def.options().length; o++) {
                question.getOptions().add(Option.builder()
                        .question(question)
                        .optionText(def.options()[o])
                        .optionIndex(o)
                        .build());
            }
            exam.getQuestions().add(question);
        }
        examRepository.save(exam);
    }

    // ── Thời gian biểu + bài tập cho hs1 ─────────────────────────────────────
    private void seedScheduleAndHomework() {
        User student = userRepository.findByEmail("hs1@edusmart.vn").orElse(null);
        if (student == null) return;

        // Ngày của entity Schedule: MON..SUN (enum riêng của project)
        List<Schedule> schedules = List.of(
                schedule("MON", "Toán học", "Thầy Bình (Phòng 302)", "08:00", "09:30", "Mang máy tính Casio"),
                schedule("MON", "Vật lý", "Cô Hằng (Phòng 205)", "09:45", "11:15", null),
                schedule("TUE", "Tiếng Anh", "Thầy David (Phòng 102)", "14:00", "15:30", "Ôn lại từ vựng Unit 5"),
                schedule("WED", "Hóa học", "Cô Lan (Phòng 301)", "08:00", "09:30", "Làm thí nghiệm nhóm 3 người"),
                schedule("FRI", "Toán học", "Thầy Bình (Phòng 302)", "08:00", "09:30", "Kiểm tra 15 phút đầu giờ")
        );
        for (Schedule s : schedules) {
            s.setUser(student);
            scheduleRepository.save(s);
        }

        LocalDateTime now = LocalDateTime.now();
        List<Homework> homework = List.of(
                Homework.builder().user(student).subject("Toán học")
                        .title("Giải 10 bài tập hàm số bậc hai — trang 45")
                        .dueDate(LocalDate.now().plusDays(1)).build(),
                Homework.builder().user(student).subject("Vật lý")
                        .title("Lập bảng tính điện trở mạch nối tiếp — trang 78")
                        .dueDate(LocalDate.now().plusDays(3)).build(),
                Homework.builder().user(student).subject("Tiếng Anh")
                        .title("Viết đoạn văn 100 từ về thì hiện tại tiếp diễn")
                        .dueDate(LocalDate.now().plusDays(2)).build(),
                Homework.builder().user(student).subject("Hóa học")
                        .title("Học thuộc 20 nguyên tố đầu của bảng tuần hoàn")
                        .dueDate(LocalDate.now().minusDays(1)).build(),
                Homework.builder().user(student).subject("Toán học")
                        .title("Ôn tập chương 1 — 20 câu trắc nghiệm")
                        .dueDate(now.minusDays(5).toLocalDate())
                        .status(Homework.HomeworkStatus.SUBMITTED)
                        .coinsEarned(20)
                        .submittedAt(now.minusDays(5)).build()
        );
        homeworkRepository.saveAll(homework);
    }

    private Schedule schedule(String day, String subject, String teacher,
                              String start, String end, String note) {
        return Schedule.builder()
                .dayOfWeek(Schedule.DayOfWeek.valueOf(day))
                .subject(subject)
                .teacher(teacher)
                .startTime(LocalTime.parse(start))
                .endTime(LocalTime.parse(end))
                .note(note)
                .build();
    }
}
