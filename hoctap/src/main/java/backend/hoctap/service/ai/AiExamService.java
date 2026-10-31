package backend.hoctap.service.ai;

import backend.hoctap.dto.response.exam.ExamResponse;
import backend.hoctap.entity.Exam;
import backend.hoctap.entity.Option;
import backend.hoctap.entity.Question;
import backend.hoctap.repository.ExamRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║          AI EXAM AGENT — tạo đề thi / bài tập từ file        ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Luồng: đọc file (PDF/DOC/DOCX/TXT) → trích text → gửi cho AI kèm prompt
 * → nhận JSON câu hỏi → validate → tạo Exam + Question + Option thật trong DB.
 *
 * 2 chế độ:
 *   EXAM     → tạo PHÒNG THI: thời gian làm bài LẤY THEO TÀI LIỆU (regex quét
 *              "90 phút"/"90 minutes" — tài liệu là nguồn sự thật, đè cả kết quả AI),
 *              câu hỏi trắc nghiệm, chấm tự động bằng existing submit flow.
 *   HOMEWORK → tạo BÀI TẬP LUYỆN TẬP: mỗi câu CÓ GIẢI THÍCH (nếu tài liệu có),
 *              không giới hạn thời gian (durationMinutes = 0).
 *
 * Người dùng có thể chọn "lớp" (Năm nhất → Năm tư, 10→12) để AI điều chỉnh độ khó.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiExamService {

    public static final String MODE_EXAM = "EXAM";
    public static final String MODE_HOMEWORK = "HOMEWORK";

    private final AiChatClient aiChatClient;
    private final FileTextExtractor fileTextExtractor;
    private final ExamRepository examRepository;

    /** Parser JSON riêng cho phản hồi của AI (Jackson 2 — không phụ thuộc bean của Boot) */
    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @Value("${ai.max-document-chars:60000}")
    private int maxDocumentChars;

    /** "Thời gian làm bài: 90 phút" / "Time allowed: 90 minutes" / "trong 60 phút" */
    private static final Pattern DURATION_PATTERN = Pattern.compile(
            "(\\d{1,3})\\s*(?:phút|minutes?|mins?)\\b", Pattern.CASE_INSENSITIVE);

    @Transactional
    public ExamResponse generateFromFile(MultipartFile file, String mode,
                                         Integer requestedDuration, Integer questionCount,
                                         String grade) {
        // ── 1. Validate đầu vào ─────────────────────────────────────────
        String effectiveMode = MODE_HOMEWORK.equalsIgnoreCase(mode) ? MODE_HOMEWORK : MODE_EXAM;
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Vui lòng chọn file tài liệu (PDF, DOC, DOCX hoặc TXT)");
        }
        if (!fileTextExtractor.supports(file.getOriginalFilename())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Định dạng file chưa hỗ trợ. Hỗ trợ: PDF, DOC, DOCX, TXT");
        }

        // ── 2. Đọc nội dung tài liệu ────────────────────────────────────
        String documentText;
        try {
            documentText = fileTextExtractor.extract(
                    file.getOriginalFilename(), file.getBytes());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Không đọc được nội dung file");
        }
        if (documentText.length() > maxDocumentChars) {
            documentText = documentText.substring(0, maxDocumentChars)
                    + "\n\n[...TÀI LIỆU BỊ CẮT BỚT PHẦN CUỐI DO QUÁ DÀI...]";
        }

        // ── 3. Quét thời gian làm bài từ chính tài liệu ──────────────────
        Integer documentDuration = findDurationInText(documentText);

        // ── 4. Gọi AI tạo câu hỏi ────────────────────────────────────────
        String systemPrompt = buildSystemPrompt(effectiveMode);
        String userPrompt = buildUserPrompt(documentText, effectiveMode,
                questionCount, grade, documentDuration, requestedDuration);

        String aiResponse = aiChatClient.chat(systemPrompt, userPrompt);

        JsonNode examJson = parseExamJson(aiResponse);
        // JSON sai cấu trúc cơ bản → hỏi lại AI một lần
        if (examJson == null || examJson.path("questions").isEmpty()) {
            aiResponse = aiChatClient.chat(systemPrompt,
                    userPrompt + "\n\n[LƯU Ý QUAN TRỌNG: Lần trước bạn trả về sai định dạng. "
                    + "Chỉ được trả về MỘT khối JSON hợp lệ, không thêm chữ nào khác.]");
            examJson = parseExamJson(aiResponse);
            if (examJson == null || examJson.path("questions").isEmpty()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI trả về dữ liệu không hợp lệ. Vui lòng thử lại.");
            }
        }

        // ── 5. Xác định thời gian phòng thi ─────────────────────────────
        // Độ ưu tiên: thời gian ghi trong tài liệu > AI trả về > người dùng nhập
        int duration = resolveDuration(effectiveMode, documentDuration,
                examJson.path("durationMinutes").asInt(0), requestedDuration);

        // ── 6. Tạo Exam + Question + Option trong DB ────────────────────
        Exam exam = buildExam(examJson, effectiveMode, duration, questionCount);

        log.info("AI Agent created exam '{}' (mode={}, {} câu, {} phút) từ file {}",
                exam.getTitle(), effectiveMode, exam.getQuestions().size(),
                duration, file.getOriginalFilename());

        examRepository.save(exam);

        return ExamResponse.builder()
                .id(exam.getId())
                .title(exam.getTitle())
                .category(exam.getCategory())
                .durationMinutes(exam.getDurationMinutes())
                .questionsCount(exam.getQuestionsCount())
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PROMPT
    // ─────────────────────────────────────────────────────────────────────

    private String buildSystemPrompt(String mode) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là chuyên gia ra đề thi trắc nghiệm tiếng Việt. ");
        sb.append("Nhiệm vụ: đọc tài liệu người dùng cung cấp và tạo bộ câu hỏi trắc nghiệm ");
        sb.append("bám sát NỘI DUNG tài liệu, đáp án đúng phải có căn cứ từ tài liệu.\n\n");
        sb.append("QUY TẮC BẮT BUỘC:\n");
        sb.append("- CHỈ trả về MỘT khối JSON hợp lệ, KHÔNG thêm bất kỳ chữ nào khác, ");
        sb.append("KHÔNG bọc trong ```json ```.\n");
        sb.append("- Cấu trúc JSON chính xác như sau:\n");
        sb.append("{\"title\": \"tên đề/thai bài tập\", \"category\": \"môn học\", ");
        sb.append("\"durationMinutes\": <số nguyên phút>, \"questions\": [");
        sb.append("{\"questionText\": \"nội dung câu hỏi\", ");
        sb.append("\"options\": [\"A\", \"B\", \"C\", \"D\"], ");
        sb.append("\"correctOptionIndex\": <0|1|2|3>, ");
        sb.append("\"explanation\": \"giải thích ngắn gọn hoặc chuỗi rỗng\"}]}\n");
        sb.append("- Mỗi câu đúng 4 lựa chọn, correctOptionIndex là index của đáp án đúng (bắt đầu từ 0).\n");
        sb.append("- Câu hỏi phải đa dạng mức độ (dễ/vận dụng) và KHÔNG trùng lặp.\n");
        if (MODE_HOMEWORK.equals(mode)) {
            sb.append("- Đây là BÀI TẬP LUYỆN TẬP: trường explanation BẮT BUỘC có nội dung ");
            sb.append("giải thích đáp án dựa trên tài liệu (nếu tài liệu có lý do/giải thì trích dẫn).\n");
            sb.append("- durationMinutes luôn để 0 (bài tập không giới hạn thời gian).\n");
        } else {
            sb.append("- Đây là ĐỀ THI: nếu tài liệu là đề thi có ghi thời gian làm bài, ");
            sb.append("durationMinutes PHẢI bằng đúng số phút đó (ví dụ đề ghi '90 phút' → 90). ");
            sb.append("Nếu là tài liệu lý thuyết không ghi thời gian, chọn thời gian hợp lý theo độ dài đề.\n");
            sb.append("- explanation có thể để chuỗi rỗng nếu tài liệu không nêu lý do.\n");
        }
        return sb.toString();
    }

    private String buildUserPrompt(String documentText, String mode,
                                   Integer questionCount, String grade,
                                   Integer documentDuration, Integer requestedDuration) {
        StringBuilder sb = new StringBuilder();
        if (grade != null && !grade.isBlank()) {
            sb.append("Đối tượng làm bài: ").append(grade);
            sb.append(" (hãy chọn từ vựng và độ khó phù hợp đối tượng này).\n");
        }
        int count = clamp(questionCount, 3, 30, 10);
        sb.append("Số lượng câu hỏi cần tạo: ").append(count).append(".\n");
        if (MODE_EXAM.equals(mode)) {
            if (documentDuration != null) {
                sb.append("Tài liệu CÓ ghi thời gian làm bài: ").append(documentDuration)
                  .append(" phút → durationMinutes phải là ").append(documentDuration).append(".\n");
            } else if (requestedDuration != null) {
                sb.append("Người dùng chọn thời gian làm bài: ").append(requestedDuration)
                  .append(" phút → durationMinutes là ").append(requestedDuration).append(".\n");
            }
        }
        sb.append("\n── NỘI DUNG TÀI LIỆU ──\n");
        sb.append(documentText);
        sb.append("\n── HẾT TÀI LIỆU ──\n");
        sb.append("Hãy tạo JSON theo đúng quy tắc hệ thống.");
        return sb.toString();
    }

    // ─────────────────────────────────────────────────────────────────────
    // PARSE + VALIDATE
    // ─────────────────────────────────────────────────────────────────────

    /** Cắt phần JSON từ câu trả lời của AI (bỏ ``` fences, chữ thừa) */
    private JsonNode parseExamJson(String aiResponse) {
        try {
            String s = aiResponse.trim();
            // Bỏ code fence nếu AI vẫn bọc
            if (s.contains("```")) {
                s = s.replaceAll("```[a-zA-Z]*", "").replace("```", "").trim();
            }
            int start = s.indexOf('{');
            int end = s.lastIndexOf('}');
            if (start < 0 || end <= start) return null;
            return objectMapper.readTree(s.substring(start, end + 1));
        } catch (Exception e) {
            log.warn("Failed to parse AI JSON: {}", e.getMessage());
            return null;
        }
    }

    private Exam buildExam(JsonNode examJson, String mode, int duration, int maxQuestions) {
        String title = truncate(examJson.path("title").asText(
                MODE_HOMEWORK.equals(mode) ? "Bài tập luyện tập — AI" : "Đề thi — AI"), 200);
        String category = MODE_HOMEWORK.equals(mode)
                ? "Bài tập AI"
                : truncate(examJson.path("category").asText("AI"), 50);

        Exam exam = Exam.builder()
                .title(title)
                .category(category)
                .durationMinutes(duration)
                .questionsCount(0)
                .build();

        JsonNode questions = examJson.path("questions");
        int added = 0;
        for (JsonNode q : questions) {
            if (added >= Math.max(maxQuestions, 30)) break; // trần an toàn 30 câu

            String questionText = q.path("questionText").asText("").trim();
            if (questionText.isEmpty()) continue;

            List<String> options = new ArrayList<>();
            for (JsonNode opt : q.path("options")) {
                String text = opt.asText("").trim();
                if (!text.isEmpty()) options.add(text);
            }
            if (options.size() < 2) continue; // câu hỏi không hợp lệ

            int correct = clamp(q.path("correctOptionIndex").asInt(0), 0, options.size() - 1);
            String explanation = q.path("explanation").asText("").trim();

            Question question = Question.builder()
                    .exam(exam)
                    .questionText(questionText)
                    .correctOptionIndex(correct)
                    .orderIndex(added + 1)
                    .explanation(explanation.isEmpty() ? null : truncate(explanation, 2000))
                    .build();

            for (int i = 0; i < options.size(); i++) {
                question.getOptions().add(Option.builder()
                        .question(question)
                        .optionText(truncate(options.get(i), 500))
                        .optionIndex(i)
                        .build());
            }
            exam.getQuestions().add(question);
            added++;
        }

        if (added == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI không tạo được câu hỏi hợp lệ từ tài liệu này. Vui lòng thử lại "
                    + "với file có nội dung rõ ràng hơn.");
        }
        exam.setQuestionsCount(added);
        return exam;
    }

    /** Thời gian phòng thi: EXAM → tài liệu > AI > người dùng; HOMEWORK → 0 (không giới hạn) */
    private int resolveDuration(String mode, Integer documentDuration,
                                int aiDuration, Integer requestedDuration) {
        if (MODE_HOMEWORK.equals(mode)) return 0;

        if (documentDuration != null && documentDuration >= 5) return documentDuration;
        if (aiDuration >= 5) return Math.min(aiDuration, 300);
        int fallback = requestedDuration != null ? requestedDuration : 15;
        return clamp(fallback, 5, 300, 15);
    }

    /** Tìm "N phút" trong text — lấy số phút đầu tiên hợp lệ (5..300) */
    private Integer findDurationInText(String text) {
        Matcher m = DURATION_PATTERN.matcher(text);
        while (m.find()) {
            try {
                int value = Integer.parseInt(m.group(1));
                if (value >= 5 && value <= 300) return value;
            } catch (NumberFormatException ignored) {}
        }
        return null;
    }

    private int clamp(Integer value, int min, int max, int fallback) {
        if (value == null) return fallback;
        return Math.max(min, Math.min(max, value));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String truncate(String s, int max) {
        if (s == null) return null;
        return s.length() <= max ? s : s.substring(0, max);
    }
}
