package backend.hoctap.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_attempts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExamAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private Exam exam;

    /**
     * Điểm số cuối cùng (thang 10.0).
     * Công thức: (correctCount / totalCount) * 10
     */
    @Column(name = "score", precision = 4, scale = 1)
    private BigDecimal score;

    @Column(name = "correct_count")
    private Integer correctCount;

    @Column(name = "total_count")
    private Integer totalCount;

    /**
     * Xu thưởng = correctCount * 15
     */
    @Column(name = "coins_earned", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer coinsEarned = 0;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 15)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @CreationTimestamp
    @Column(name = "started_at", updatable = false)
    private LocalDateTime startedAt;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    // ── Enum ───────────────────────────────────────────────────────────────
    public enum AttemptStatus {
        IN_PROGRESS, COMPLETED, TIMED_OUT
    }
}
