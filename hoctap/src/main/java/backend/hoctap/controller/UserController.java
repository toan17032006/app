package backend.hoctap.controller;

import backend.hoctap.dto.request.user.ChangePasswordRequest;
import backend.hoctap.dto.request.user.UpdateProfileRequest;
import backend.hoctap.dto.response.common.ApiResponse;
import backend.hoctap.dto.response.user.UserResponse;
import backend.hoctap.dto.response.user.UserStatsResponse;
import backend.hoctap.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GET  /api/users/{id}         → Lấy profile
 * GET  /api/users/{id}/stats   → Thống kê dashboard
 * PUT  /api/users/{id}         → Cập nhật profile
 * PUT  /api/users/{id}/password → Đổi mật khẩu
 * DELETE /api/users/{id}       → Xóa tài khoản
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(id)));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getStats(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(userService.getStats(id)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProfileRequest req) {

        return ResponseEntity.ok(
                ApiResponse.success("Cập nhật thành công", userService.updateProfile(id, req)));
    }

    /**
     * PUT /api/users/5/password
     * Body: { "currentPassword": "...", "newPassword": "...", "confirmPassword": "..." }
     */
    @PutMapping("/{id}/password")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @PathVariable Long id,
            @Valid @RequestBody ChangePasswordRequest req) {

        userService.changePassword(id, req);
        return ResponseEntity.ok(ApiResponse.success("Đổi mật khẩu thành công", null));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAccount(@PathVariable Long id) {
        userService.deleteAccount(id);
        return ResponseEntity.noContent().build();
    }
}
