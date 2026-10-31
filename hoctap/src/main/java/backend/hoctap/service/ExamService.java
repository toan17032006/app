package backend.hoctap.service;

import backend.hoctap.dto.request.exam.ExamRequest;
import backend.hoctap.dto.request.exam.SubmitExamRequest;
import backend.hoctap.dto.response.exam.*;
import backend.hoctap.entity.*;
import backend.hoctap.entity.ExamAttempt.AttemptStatus;
import backend.hoctap.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    EXAM SERVICE                             ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Service phức tạp nhất trong dự án.
 * Có 3 luồng chính:
 *
 *   1. getExams()       → Xem danh sách bài thi
 *   2. startExam()      → Bắt đầu làm bài (tạo ExamAttempt)
 *   3. submitExam()     → Nộp bài, chấm điểm, cộng xu
 *
 * Bảo mật: correctOptionIndex KHÔNG được trả về khi lấy câu hỏi.
 *          Chỉ tiết lộ sau khi học sinh đã nộp bài.
 */
@Service
@RequiredArgsConstructor
public class ExamService {

    private final ExamRepository        examRepository;
    private final QuestionRepository    questionRepository;
    private final OptionRepository      optionRepository;
    private final ExamAttemptRepository examAttemptRepository;
    private final UserRepository        userRepository;
    private final UserService           userService;

    // ─────────────────────────────────────────────────────────────────────────
    // READ — Danh sách bài thi
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExamResponse> getAllExams() {
        return examRepository.findAll()
                .stream()
                .map(this::toExamResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ExamResponse> getByCategory(String category) {
        return examRepository.findByCategoryOrderByCreatedAtDesc(category)
                .stream()
                .map(this::toExamResponse)
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // START EXAM — Học sinh bắt đầu làm bài
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo một lần làm bài mới (ExamAttempt).
     *
     * Nếu học sinh đã có lần làm ĐANG TIẾN HÀNH (IN_PROGRESS) → tiếp tục lần đó.
     * Nếu chưa → tạo mới.
     *
     * Khi trả câu hỏi: KHÔNG có correctOptionIndex (bảo mật).
     */
    @Transactional
    public ExamDetailResponse startExam(Long userId, Long examId) {
        // Tìm bài thi kèm câu hỏi + đáp án (1 query duy nhất, tránh N+1)
        Exam exam = examRepository.findByIdWithQuestionsAndOptions(examId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy bài thi"));

        User user = userService.findOrThrow(userId);

        // Kiểm tra có lần làm đang tiến hành không → nếu có thì dùng lại
        ExamAttempt attempt = examAttemptRepository
                .findByUserIdAndExamIdAndStatus(userId, examId, AttemptStatus.IN_PROGRESS)
                .orElseGet(() -> {
                    // Tạo attempt mới
                    ExamAttempt newAttempt = ExamAttempt.builder()
                            .user(user)
                            .exam(exam)
                            .totalCount(exam.getQuestionsCount())
                            .status(AttemptStatus.IN_PROGRESS)
                            .build();
                    return examAttemptRepository.save(newAttempt);
                });

        // Map câu hỏi sang DTO (KHÔNG có correctOptionIndex)
        // Options được load theo từng câu (OptionRepository) — tránh fetch 2 bag cùng lúc
        List<QuestionResponse> questions = exam.getQuestions()
                .stream()
                .sorted(java.util.Comparator.comparing(
                        Question::getOrderIndex,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .map(q -> QuestionResponse.builder()
                        .id(q.getId())
                        .content(q.getQuestionText())
                        .type("SINGLE_CHOICE")
                        .orderIndex(q.getOrderIndex())
                        .options(optionRepository
                                .findByQuestionIdOrderByOptionIndexAsc(q.getId())
                                .stream()
                                .map(o -> OptionResponse.builder()
                                        .id(o.getId())
                                        .optionIndex(o.getOptionIndex())
                                        .content(o.getOptionText())
                                        .build())
                                .toList())
                        .build())
                .toList();

        return ExamDetailResponse.builder()
                .examId(examId)
                .attemptId(attempt.getId())
                .title(exam.getTitle())
                .category(exam.getCategory())
                .durationMinutes(exam.getDurationMinutes())
                .questions(questions)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // SUBMIT EXAM — Nộp bài, chấm điểm
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Đây là phần phức tạp nhất — luồng chấm điểm:
     *
     *   1. Lấy attempt, kiểm tra userId + status
     *   2. Tải danh sách câu hỏi kèm đáp án đúng
     *   3. So sánh từng câu trả lời của học sinh với đáp án đúng
     *   4. Tính điểm: score = (correctCount / total) × 10
     *   5. Tính xu: coinsEarned = correctCount × 15
     *   6. Cộng xu vào tài khoản user
     *   7. Trả về kết quả KÈM đáp án đúng (lúc này mới được tiết lộ)
     *
     * @param timedOut true nếu server phát hiện hết giờ
     */
    @Transactional
    public ExamResultResponse submitExam(Long userId,
                                          SubmitExamRequest req,
                                          boolean timedOut) {
        // Bước 1: Tìm và xác thực attempt
        ExamAttempt attempt = examAttemptRepository.findById(req.getAttemptId())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Không tìm thấy lần làm bài"));

        if (!attempt.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Bạn không có quyền nộp bài này");
        }

        if (attempt.getStatus() != AttemptStatus.IN_PROGRESS) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Bài thi này đã được nộp rồi");
        }

        // Bước 2: Lấy câu hỏi kèm đáp án đúng (lần này mới load correctOptionIndex)
        Exam exam = attempt.getExam();
        List<Question> questions = questionRepository
                .findByExamIdWithOptions(exam.getId());

        // Bước 3: Tạo map từ questionId → đáp án học sinh chọn
        // Map<questionId, selectedIndex>
        Map<Long, Integer> answerMap = req.getAnswers().stream()
                .filter(a -> a.getSelectedOptionIndex() != null)
                .collect(Collectors.toMap(
                        SubmitExamRequest.AnswerItem::getQuestionId,
                        SubmitExamRequest.AnswerItem::getSelectedOptionIndex
                ));

        // Bước 4: Chấm điểm từng câu
        int correctCount = 0;
        List<ExamResultResponse.AnswerResultItem> resultItems = new ArrayList<>();

        for (Question q : questions) {
            Integer selected = answerMap.get(q.getId());
            boolean isCorrect = selected != null
                    && selected.equals(q.getCorrectOptionIndex());
            if (isCorrect) correctCount++;

            resultItems.add(ExamResultResponse.AnswerResultItem.builder()
                    .questionId(q.getId())
                    .questionText(q.getQuestionText())
                    .selectedIndex(selected)
                    .correctIndex(q.getCorrectOptionIndex())   // ✅ Lúc này mới tiết lộ
                    .isCorrect(isCorrect)
                    .build());
        }

        int total = questions.size();

        // Bước 5: Tính điểm (thang 10, làm tròn 1 chữ số thập phân)
        BigDecimal score = total > 0
                ? BigDecimal.valueOf((double) correctCount / total * 10)
                        .setScale(1, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        int coinsEarned = correctCount * 15;

        // Bước 6: Cập nhật attempt
        attempt.setScore(score);
        attempt.setCorrectCount(correctCount);
        attempt.setTotalCount(total);
        attempt.setCoinsEarned(coinsEarned);
        attempt.setStatus(timedOut ? AttemptStatus.TIMED_OUT : AttemptStatus.COMPLETED);
        attempt.setSubmittedAt(LocalDateTime.now());

        // Bước 7: Cộng xu (trong cùng @Transactional → an toàn)
        if (coinsEarned > 0) {
            userService.addCoins(userId, coinsEarned);
        }

        return ExamResultResponse.builder()
                .attemptId(attempt.getId())
                .examTitle(exam.getTitle())
                .score(score)
                .correctCount(correctCount)
                .totalCount(total)
                .coinsEarned(coinsEarned)
                .status(attempt.getStatus().name())
                .submittedAt(attempt.getSubmittedAt())
                .answers(resultItems)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HISTORY
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<ExamAttemptResponse> getHistory(Long userId) {
        return examAttemptRepository
                .findByUserIdOrderByStartedAtDesc(userId)
                .stream()
                .map(a -> ExamAttemptResponse.builder()
                        .id(a.getId())
                        .examId(a.getExam().getId())
                        .examTitle(a.getExam().getTitle())
                        .category(a.getExam().getCategory())
                        .score(a.getScore())
                        .correctCount(a.getCorrectCount())
                        .totalCount(a.getTotalCount())
                        .coinsEarned(a.getCoinsEarned())
                        .status(a.getStatus().name())
                        .startedAt(a.getStartedAt())
                        .submittedAt(a.getSubmittedAt())
                        .build())
                .toList();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // CREATE EXAM (Admin)
    // ─────────────────────────────────────────────────────────────────────────

    @Transactional
    public ExamResponse createExam(ExamRequest req) {
        Exam exam = Exam.builder()
                .title(req.getTitle())
                .category(req.getCategory() != null ? req.getCategory() : "Chung")
                .durationMinutes(req.getDurationMinutes())
                .questionsCount(req.getQuestions() != null ? req.getQuestions().size() : 0)
                .build();

        Exam savedExam = examRepository.save(exam);

        // Tạo câu hỏi và đáp án
        if (req.getQuestions() != null) {
            for (int i = 0; i < req.getQuestions().size(); i++) {
                ExamRequest.QuestionRequest qReq = req.getQuestions().get(i);

                // Tìm correctOptionIndex từ danh sách options
                int correctIdx = 0;
                for (int j = 0; j < qReq.getOptions().size(); j++) {
                    if (Boolean.TRUE.equals(qReq.getOptions().get(j).getIsCorrect())) {
                        correctIdx = j;
                        break;
                    }
                }

                Question question = Question.builder()
                        .exam(savedExam)
                        .questionText(qReq.getContent())
                        .correctOptionIndex(correctIdx)
                        .orderIndex(i)
                        .build();

                Question savedQ = questionRepository.save(question);

                // Tạo từng đáp án
                for (int j = 0; j < qReq.getOptions().size(); j++) {
                    Option option = Option.builder()
                            .question(savedQ)
                            .optionText(qReq.getOptions().get(j).getContent())
                            .optionIndex(j)
                            .build();
                    optionRepository.save(option);
                }
            }
        }

        return toExamResponse(savedExam);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private ExamResponse toExamResponse(Exam e) {
        return ExamResponse.builder()
                .id(e.getId())
                .title(e.getTitle())
                .category(e.getCategory())
                .durationMinutes(e.getDurationMinutes())
                .questionsCount(e.getQuestionsCount())
                .createdAt(e.getCreatedAt())
                .build();
    }
}
