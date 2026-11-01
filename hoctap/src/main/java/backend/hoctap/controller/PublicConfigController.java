package backend.hoctap.controller;

import backend.hoctap.dto.response.common.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Config công khai cho frontend (không cần đăng nhập).
 *
 * GET /api/config/google → { googleClientId: "xxx.apps.googleusercontent.com" }
 *
 * Frontend (GoogleButton) tự lấy Client ID từ đây khi khởi động — nhờ vậy
 * khi đổi/thêm Client ID chỉ cần đặt biến môi trường GOOGLE_CLIENT_ID và
 * restart backend, KHÔNG cần build lại frontend.
 */
@RestController
@RequestMapping("/api/config")
public class PublicConfigController {

    @Value("${google.client-id:}")
    private String googleClientId;

    @GetMapping("/google")
    public ResponseEntity<ApiResponse<Map<String, String>>> google() {
        String clientId = googleClientId != null ? googleClientId.trim() : "";
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("googleClientId", clientId)));
    }
}
