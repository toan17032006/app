package backend.hoctap.dto.response.auth;

import backend.hoctap.dto.response.user.UserResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String accessToken;
    private String refreshToken;

    @Builder.Default
    private String tokenType = "Bearer";

    /** Số giây token hết hạn */
    private Long expiresIn;

    /** Thông tin user vừa đăng nhập / đăng ký */
    private UserResponse user;
}
