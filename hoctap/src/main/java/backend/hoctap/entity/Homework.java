package backend.hoctap.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "homework")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Homework {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "subject", nullable = false, length = 50)
    private String subject;

    @Column(name = "title", nullable = false, length = 200)
    private String title;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    @Builder.Default
    private HomeworkStatus status = HomeworkStatus.PENDING;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    /**
     * Xu thưởng khi nộp bài = +20
     */
    @Column(name = "coins_earned", columnDefinition = "INT DEFAULT 0")
    @Builder.Default
    private Integer coinsEarned = 0;

    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // ── Enum ───────────────────────────────────────────────────────────────
    public enum HomeworkStatus {
        PENDING, SUBMITTED
    }
}
