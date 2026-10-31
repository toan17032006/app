package backend.hoctap.dto.response.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {

    private Long id;
    private String fullName;
    private String email;
    private String grade;
    private BigDecimal gpa;
    private Integer coins;
    private String avatarUrl;

    /** "EMAIL" hoặc "GOOGLE" */
    private String authProvider;

    private LocalDateTime createdAt;

    // KHÔNG bao gồm: passwordHash, googleId
}
