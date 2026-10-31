package backend.hoctap.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    @Column(name = "question_text", nullable = false, columnDefinition = "TEXT")
    private String questionText;

    /**
     * Index (0-based) của đáp án đúng trong danh sách options.
     * Ví dụ: 0 = A, 1 = B, 2 = C, 3 = D
     */
    @Column(name = "correct_option_index", nullable = false)
    private Integer correctOptionIndex;

    @Column(name = "order_index", nullable = false)
    private Integer orderIndex;

    /**
     * Giải thích đáp án (tùy chọn) — AI Agent tạo bài tập sẽ điền trường này,
     * hiển thị cho học sinh sau khi nộp bài.
     */
    @Column(name = "explanation", columnDefinition = "TEXT")
    private String explanation;

    // ── Relationships ──────────────────────────────────────────────────────
    @OneToMany(mappedBy = "question", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("optionIndex ASC")
    @Builder.Default
    private List<Option> options = new ArrayList<>();
}
