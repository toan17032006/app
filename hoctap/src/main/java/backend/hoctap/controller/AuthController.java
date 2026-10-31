package backend.hoctap.controller;

import backend.hoctap.dto.request.auth.*;
import backend.hoctap.dto.response.auth.AuthResponse;
import backend.hoctap.dto.response.common.ApiResponse;
import backend.hoctap.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    AUTH CONTROLLER                          ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Tất cả endpoint ở đây đều PUBLIC (không cần JWT).
 * Đã cấu hình trong SecurityConfig:
 *   .requestMatchers("/api/auth/**").permitAll()
 *
 * Endpoint map:
 *
 *   POST /api/auth/register        → Đăng ký tài khoản
 *   POST /api/auth/login           → Đăng nhập email/password
 *   POST /api/auth/google          → Đăng nhập bằng Google
 *   POST /api/auth/refresh         → Làm mới token
 *   POST /api/auth/forgot-password → Gửi email đặt lại mật khẩu
 *   POST /api/auth/reset-password  → Đặt mật khẩu mới
 *
 * ─── Lưu ý về HTTP Status Codes ───────────────────────────────
 *
 *  200 OK         → Thành công (GET, PUT thông thường)
 *  201 Created    → Tạo mới thành công (POST tạo resource)
 *  400 Bad Request→ Client gửi data sai
 *  401 Unauthorized → Chưa xác thực (thiếu/sai token)
 *  403 Forbidden  → Đã xác thực nhưng không có quyền
 *  404 Not Found  → Không tìm thấy resource
 *  409 Conflict   → Xung đột (email đã tồn tại)
 *  500 Internal Server Error → Lỗi server
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // ─────────────────────────────────────────────────────────────────────────
    // ĐĂNG KÝ
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/register
     *
     * Request body:
     * {
     *   "fullName": "Nguyễn Văn A",
     *   "email":    "a@gmail.com",
     *   "password": "Password123",
     *   "grade":    "12A1"
     * }
     *
     * Response 201:
     * {
     *   "success": true,
     *   "data": {
     *     "accessToken":  "eyJ...",
     *     "refreshToken": "eyJ...",
     *     "tokenType":    "Bearer",
     *     "expiresIn":    86400,
     *     "user": { "id": 1, "fullName": "...", ... }
     *   }
     * }
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest req) {

        AuthResponse response = authService.register(req);
        return ResponseEntity
                .status(HttpStatus.CREATED)  // 201 — tạo user mới
                .body(ApiResponse.success("Đăng ký thành công! Chào mừng bạn đến với EduSmart 🎉", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ĐĂNG NHẬP EMAIL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/login
     *
     * Request body:
     * {
     *   "email":    "a@gmail.com",
     *   "password": "Password123"
     * }
     */
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest req) {

        AuthResponse response = authService.login(req);
        return ResponseEntity.ok(
                ApiResponse.success("Đăng nhập thành công!", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ĐĂNG NHẬP GOOGLE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/google
     *
     * Frontend gửi Google ID Token sau khi user click "Sign in with Google".
     *
     * Request body:
     * {
     *   "idToken": "eyJhbGciOiJSUzI1NiJ9..."  ← Token từ Google SDK
     * }
     *
     * Response: giống /login — trả về JWT của hệ thống (không phải Google token)
     */
    @PostMapping("/google")
    public ResponseEntity<ApiResponse<AuthResponse>> loginWithGoogle(
            @Valid @RequestBody GoogleLoginRequest req) {

        AuthResponse response = authService.loginWithGoogle(req);
        return ResponseEntity.ok(
                ApiResponse.success("Đăng nhập Google thành công!", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // REFRESH TOKEN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/refresh
     *
     * Khi access token hết hạn, frontend gửi refresh token để lấy cặp mới.
     *
     * Request body:
     * {
     *   "refreshToken": "eyJ..."
     * }
     *
     * Frontend nên tự động gọi endpoint này khi nhận 401 từ các API khác,
     * thay vì bắt user đăng nhập lại.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(
            @RequestBody java.util.Map<String, String> body) {

        String refreshToken = body.get("refreshToken");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("refreshToken không được để trống"));
        }

        AuthResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(ApiResponse.success("Làm mới token thành công", response));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // QUÊN MẬT KHẨU / ĐẶT LẠI MẬT KHẨU
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * POST /api/auth/forgot-password
     *
     * Tạo token đặt lại mật khẩu. Vì hệ thống chưa cấu hình mail server
     * (app.mail.enabled=false), token được trả về trong data.resetToken —
     * frontend dùng token đó tạo form "Đặt lại mật khẩu" tại trang /forgot-password.
     *
     * Khi tích hợp mail (app.mail.enabled=true): token chỉ được gửi qua email,
     * data.resetToken luôn là null.
     *
     * Request body: { "email": "a@gmail.com" }
     *
     * Response 200: { "success": true, "data": { "resetToken": "abc..." | null } }
     * → Luôn trả 200 kể cả khi email không tồn tại (chống dò email).
     */
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiResponse<java.util.Map<String, String>>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest req) {

        String resetToken = authService.createPasswordResetToken(req.getEmail());
        String message = "Nếu email tồn tại trong hệ thống, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu.";

        java.util.Map<String, String> data = resetToken != null
                ? java.util.Map.of("resetToken", resetToken)
                : java.util.Map.<String, String>of();

        return ResponseEntity.ok(ApiResponse.success(message, data));
    }

    /**
     * POST /api/auth/reset-password
     *
     * Đặt mật khẩu mới bằng reset token (hết hạn sau 30 phút, dùng 1 lần).
     *
     * Request body:
     * {
     *   "token":           "reset-token-from-email",
     *   "newPassword":     "NewPassword123",
     *   "confirmPassword": "NewPassword123"
     * }
     */
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest req) {

        authService.resetPassword(req.getToken(), req.getNewPassword(), req.getConfirmPassword());
        return ResponseEntity.ok(
                ApiResponse.success("Đặt lại mật khẩu thành công! Vui lòng đăng nhập lại.", null));
    }
}
