package backend.hoctap.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * Token dùng một lần để đặt lại mật khẩu (luồng "Quên mật khẩu").
 *
 * Vì hệ thống chưa cấu hình mail server, token được trả về trực tiếp
 * cho client (xem AuthController.forgotPassword) → người dùng tạo link
 * reset tại trang /forgot-password.
 *
 * Khi tích hợp mail: chỉ cần gửi token này qua email và KHÔNG trả về
 * trong response nữa (app.mail.enabled=true).
 */
@Entity
@Table(name = "password_reset_tokens")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token", nullable = false, unique = true, length = 100)
    private String token;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    /** Đánh dấu đã dùng để tránh dùng lại 2 lần */
    @Column(name = "used", nullable = false)
    @Builder.Default
    private boolean used = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** Thời gian sống của token: 30 phút */
    public static final long VALIDITY_MINUTES = 30;

    public boolean isExpired() {
        return expiresAt.isBefore(LocalDateTime.now());
    }

    public boolean isValid() {
        return !used && !isExpired();
    }
}
