package backend.hoctap.service;

import backend.hoctap.dto.request.auth.*;
import backend.hoctap.dto.response.auth.AuthResponse;
import backend.hoctap.dto.response.user.UserResponse;
import backend.hoctap.entity.PasswordResetToken;
import backend.hoctap.entity.User;
import backend.hoctap.entity.User.AuthProvider;
import backend.hoctap.repository.PasswordResetTokenRepository;
import backend.hoctap.repository.UserRepository;
import backend.hoctap.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    AUTH SERVICE                             ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Xử lý 3 luồng đăng nhập:
 *
 * 1. ĐĂNG KÝ (email + password)
 *    Client gửi: fullName, email, password, grade
 *    → Validate → Hash password → Lưu DB → Trả JWT
 *
 * 2. ĐĂNG NHẬP (email + password)
 *    Client gửi: email, password
 *    → Tìm user → So sánh password hash → Trả JWT
 *
 * 3. ĐĂNG NHẬP GOOGLE (OAuth2)
 *    Client gửi: Google ID Token (nhận từ Google SDK trên frontend)
 *    → Gửi token lên Google để verify
 *    → Lấy thông tin user từ Google
 *    → Tìm hoặc tạo user trong DB
 *    → Trả JWT của hệ thống (không phải Google token!)
 *
 * Tại sao không dùng Google token làm JWT?
 * → Vì Google token chứa thông tin của Google, không phải của hệ thống bạn.
 *   Bạn cần JWT riêng để kiểm soát: thời hạn, quyền, thông tin tùy chỉnh.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil         jwtUtil;
    private final UserService     userService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;

    /**
     * Khi mail server CHƯA được cấu hình (app.mail.enabled=false — mặc định),
     * token reset được trả về trong response để UI tạo link đặt lại mật khẩu.
     * Khi có mail (app.mail.enabled=true) → token chỉ được gửi qua email.
     */
    @Value("${app.mail.enabled:false}")
    private boolean mailEnabled;

    /**
     * OAuth 2.0 Client ID của app (biến môi trường GOOGLE_CLIENT_ID).
     * Khi có cấu hình: token Google phải được cấp CHO ĐÚNG app này
     * (kiểm tra claim "aud") — chặn token của app khác giả mạo đăng nhập.
     * Để trống (dev) → chấp nhận mọi token Google hợp lệ.
     */
    @Value("${google.client-id:}")
    private String googleClientId;

    // Google API endpoint để verify ID Token
    private static final String GOOGLE_TOKEN_INFO_URL =
            "https://oauth2.googleapis.com/tokeninfo?id_token=";

    private static final SecureRandom TOKEN_RANDOM = new SecureRandom();

    // ─────────────────────────────────────────────────────────────────────────
    // ĐĂNG KÝ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Luồng đăng ký tài khoản mới bằng email.
     *
     * Bước 1: Kiểm tra email đã tồn tại chưa
     * Bước 2: Hash mật khẩu bằng BCrypt
     * Bước 3: Tạo User entity và lưu vào DB
     * Bước 4: Tạo JWT token và trả về client
     *
     * @Transactional → Nếu bất kỳ bước nào lỗi → rollback toàn bộ.
     *   Ví dụ: lưu user thành công nhưng lỗi khi tạo token
     *   → user bị xóa khỏi DB → client nhận lỗi → thử lại được.
     */
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        // Bước 1: Kiểm tra email đã có chưa
        if (userRepository.findByEmail(req.getEmail()).isPresent()) {
            // 409 Conflict → phù hợp hơn 400 Bad Request cho trường hợp này
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Email đã được sử dụng. Vui lòng dùng email khác.");
        }

        // Bước 2 & 3: Hash password và tạo User
        // encode() → bcrypt hash: "$2a$10$..." (không thể giải mã ngược)
        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .passwordHash(passwordEncoder.encode(req.getPassword()))
                .grade(req.getGrade())
                .authProvider(AuthProvider.EMAIL)
                .coins(0)
                .build();

        User savedUser = userRepository.save(user);

        // Bước 4: Tạo JWT và trả về
        return buildAuthResponse(savedUser);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ĐĂNG NHẬP bằng Email + Password
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tại sao thông báo lỗi chung chung "Email hoặc mật khẩu không đúng"?
     * → Bảo mật! Nếu tách ra "Email không tồn tại" / "Sai mật khẩu"
     *   → hacker biết email nào tồn tại trong hệ thống → dễ tấn công hơn.
     */
    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest req) {
        // Tìm user theo email
        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "Email hoặc mật khẩu không đúng"));

        // Kiểm tra user có đăng ký bằng email không (không phải Google)
        if (user.getAuthProvider() != AuthProvider.EMAIL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tài khoản này đã đăng nhập bằng Google. Vui lòng dùng nút 'Đăng nhập bằng Google'.");
        }

        // So sánh password: matches("raw", "hash") → true/false
        // Không cần giải mã hash — BCrypt ký lại và so sánh
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Email hoặc mật khẩu không đúng");
        }

        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ĐĂNG NHẬP bằng Google OAuth2
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Luồng Google OAuth:
     *
     * Frontend (Next.js):
     *   1. Hiện nút "Đăng nhập bằng Google"
     *   2. User click → Google OAuth popup
     *   3. User chọn tài khoản Google → Google trả về ID Token
     *   4. Frontend gửi ID Token lên backend: POST /api/auth/google
     *
     * Backend (đây):
     *   5. Gửi ID Token lên Google để xác thực thật
     *   6. Google trả về thông tin: email, name, picture...
     *   7. Tìm hoặc tạo user trong DB
     *   8. Tạo JWT của hệ thống và trả về frontend
     *
     * Tại sao phải verify với Google?
     * → Vì client có thể giả mạo ID Token.
     *   Chỉ Google mới biết token nào họ cấp là thật.
     */
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest req) {
        // Bước 5: Gọi Google API để verify token
        Map<String, String> googleInfo = verifyGoogleToken(req.getIdToken());

        String email    = googleInfo.get("email");
        String name     = googleInfo.get("name");
        String googleId = googleInfo.get("sub");     // "sub" = Google user ID
        String picture  = googleInfo.get("picture"); // URL ảnh đại diện

        if (email == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Không thể xác thực tài khoản Google");
        }

        // Bước 7: Tìm user theo email → nếu có thì update, không thì tạo mới
        Optional<User> existingUser = userRepository.findByEmail(email);

        User user;
        if (existingUser.isPresent()) {
            // User đã tồn tại → cập nhật googleId và avatar
            user = existingUser.get();
            user.setGoogleId(googleId);
            if (picture != null && user.getAvatarUrl() == null) {
                user.setAvatarUrl(picture);
            }
        } else {
            // Tạo user mới từ thông tin Google
            // Không có passwordHash vì đăng nhập bằng Google
            user = User.builder()
                    .fullName(name != null ? name : email.split("@")[0])
                    .email(email)
                    .googleId(googleId)
                    .avatarUrl(picture)
                    .authProvider(AuthProvider.GOOGLE)
                    .coins(0)
                    .grade("Chưa cập nhật")
                    .build();
            user = userRepository.save(user);
        }

        // Bước 8: Tạo JWT hệ thống
        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Khi access token hết hạn (24h), client dùng refresh token
     * để lấy cặp token mới mà không cần đăng nhập lại.
     *
     * Luồng:
     *   Client → POST /api/auth/refresh { refreshToken: "..." }
     *   Backend → Verify refresh token → Trả cặp token mới
     */
    @Transactional(readOnly = true)
    public AuthResponse refreshToken(String refreshToken) {
        // Kiểm tra đây là refresh token (không phải access token)
        String type = jwtUtil.extractType(refreshToken);
        if (!"refresh".equals(type)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Token không hợp lệ");
        }

        String email = jwtUtil.extractEmail(refreshToken);

        if (!jwtUtil.isTokenValid(refreshToken, email)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Refresh token đã hết hạn. Vui lòng đăng nhập lại.");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Người dùng không tồn tại"));

        return buildAuthResponse(user);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUÊN / ĐẶT LẠI MẬT KHẨU
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo token đặt lại mật khẩu cho email.
     *
     * Luồng:
     *   1. Tìm user theo email
     *   2. Tài khoản Google → báo lỗi (Google không có mật khẩu ở hệ thống này)
     *   3. Vô hiệu các token cũ → tạo token ngẫu nhiên 32 ký tự, hết hạn sau 30 phút
     *   4. Trả về token (khi chưa có mail server) hoặc null (khi đã có mail)
     *
     * Trả về null thay vì ném lỗi khi email không tồn tại → tránh dò email.
     */
    @Transactional
    public String createPasswordResetToken(String email) {
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isEmpty()) {
            return null; // Không tiết lộ email có tồn tại hay không
        }

        User user = userOpt.get();
        if (user.getAuthProvider() != AuthProvider.EMAIL) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Tài khoản này đăng nhập bằng Google. Vui lòng dùng nút 'Đăng nhập bằng Google'.");
        }

        // Vô hiệu token cũ của user — chỉ token mới nhất có hiệu lực
        passwordResetTokenRepository.invalidateAllForUser(user.getId());

        // Token ngẫu nhiên 32 ký tự hex (128-bit entropy)
        byte[] bytes = new byte[16];
        TOKEN_RANDOM.nextBytes(bytes);
        StringBuilder sb = new StringBuilder(32);
        for (byte b : bytes) sb.append(String.format("%02x", b));

        PasswordResetToken token = PasswordResetToken.builder()
                .user(user)
                .token(sb.toString())
                .expiresAt(LocalDateTime.now().plusMinutes(PasswordResetToken.VALIDITY_MINUTES))
                .used(false)
                .build();

        passwordResetTokenRepository.save(token);

        // Khi đã tích hợp mail: gửi token qua email và return null ở đây
        return mailEnabled ? null : token.getToken();
    }

    /**
     * Đặt mật khẩu mới bằng reset token.
     *
     * Kiểm tra: token tồn tại → chưa dùng → chưa hết hạn → mật khẩu khớp xác nhận.
     * Sau đó hash mật khẩu mới, đánh dấu token đã dùng.
     */
    @Transactional
    public void resetPassword(String token, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Mật khẩu nhập lại không khớp");
        }

        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Token đặt lại mật khẩu không hợp lệ"));

        if (!resetToken.isValid()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    resetToken.isExpired()
                            ? "Token đã hết hạn. Vui lòng yêu cầu đặt lại mật khẩu mới."
                            : "Token đã được sử dụng. Vui lòng yêu cầu đặt lại mật khẩu mới.");
        }

        User user = resetToken.getUser();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo AuthResponse với cặp token access + refresh.
     * Dùng chung cho cả register, login, loginWithGoogle.
     */
    private AuthResponse buildAuthResponse(User user) {
        String accessToken  = jwtUtil.generateAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId(), user.getEmail());

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)   // 24 giờ (giây)
                .user(userService.toResponse(user))
                .build();
    }

    /**
     * Gọi Google API để verify ID Token.
     * Trả về map thông tin user từ Google.
     *
     * Trong production nên dùng Google API Client Library thay vì RestTemplate.
     * Nhưng cách này đơn giản và dễ hiểu hơn cho mục đích học.
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> verifyGoogleToken(String idToken) {
        try {
            RestTemplate restTemplate = new RestTemplate();
            // Google trả về JSON với các field: email, name, sub, picture, ...
            Map<String, String> response = restTemplate.getForObject(
                    GOOGLE_TOKEN_INFO_URL + idToken, Map.class);

            if (response == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Không thể xác thực Google token");
            }

            // Kiểm tra token có hợp lệ không (Google trả về "error" nếu sai)
            if (response.containsKey("error")) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                        "Google token không hợp lệ hoặc đã hết hạn");
            }

            // Khi đã cấu hình GOOGLE_CLIENT_ID: token phải được cấp cho đúng app này
            String expectedAud = googleClientId != null ? googleClientId.trim() : "";
            if (!expectedAud.isEmpty()) {
                String aud = response.get("aud");
                if (!expectedAud.equals(aud)) {
                    throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                            "Token Google không thuộc ứng dụng này (audience không khớp). "
                            + "Hãy đăng nhập bằng tài khoản Google của app EduSmart.");
                }
            }

            return response;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // Google trả 400/401 → token giả/hết hạn (không phải lỗi mạng)
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Google token không hợp lệ hoặc đã hết hạn");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,
                    "Không thể kết nối đến Google để xác thực");
        }
    }
}
