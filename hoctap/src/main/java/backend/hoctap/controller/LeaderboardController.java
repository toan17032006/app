package backend.hoctap.controller;

import backend.hoctap.dto.response.common.ApiResponse;
import backend.hoctap.dto.response.leaderboard.LeaderboardResponse;
import backend.hoctap.service.LeaderboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                  LEADERBOARD CONTROLLER                     ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * @RestController = @Controller + @ResponseBody
 *   → Mọi method đều tự động chuyển return value thành JSON.
 *   → Không cần @ResponseBody trên từng method.
 *
 * @RequestMapping("/api/leaderboard")
 *   → Tất cả endpoint trong class này đều có prefix /api/leaderboard
 *
 * ResponseEntity<T>
 *   → Wrapper cho phép kiểm soát HTTP status code + headers + body.
 *   → ResponseEntity.ok(data)  → 200 OK với body là data
 *   → ResponseEntity.created(uri).body(data) → 201 Created
 */
@RestController
@RequestMapping("/api/leaderboard")
@RequiredArgsConstructor
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * GET /api/leaderboard?currentUserId=5&period=all
     * Lấy toàn bộ bảng xếp hạng.
     *
     * @param period all (mặc định) | week | month — kỳ xếp hạng
     */
    @GetMapping
    public ResponseEntity<ApiResponse<List<LeaderboardResponse>>> getLeaderboard(
            @RequestParam(required = false) Long currentUserId,
            @RequestParam(defaultValue = "all") String period) {

        List<LeaderboardResponse> data = currentUserId != null
                ? leaderboardService.getFullLeaderboard(currentUserId, period)
                : leaderboardService.getTopLeaderboard(10, period);

        // ApiResponse.success() → wrapper chuẩn { success: true, data: [...] }
        return ResponseEntity.ok(ApiResponse.success(data));
    }

    /**
     * GET /api/leaderboard/top?limit=5&period=week
     * Lấy top N học sinh.
     *
     * @param period all (mặc định) | week | month — kỳ xếp hạng.
     *                week = 7 ngày gần nhất, month = từ đầu tháng hiện tại.
     */
    @GetMapping("/top")
    public ResponseEntity<ApiResponse<List<LeaderboardResponse>>> getTop(
            @RequestParam(defaultValue = "10") int limit,
            @RequestParam(defaultValue = "all") String period) {

        return ResponseEntity.ok(
                ApiResponse.success(leaderboardService.getTopLeaderboard(limit, period)));
    }

    /**
     * GET /api/leaderboard/rank/{userId}
     * Lấy thứ hạng của một user.
     *
     * @PathVariable → Lấy giá trị từ URL path.
     * Ví dụ: GET /api/leaderboard/rank/5 → userId = 5
     */
    @GetMapping("/rank/{userId}")
    public ResponseEntity<ApiResponse<Integer>> getUserRank(
            @PathVariable Long userId) {

        int rank = leaderboardService.getUserRank(userId);
        return ResponseEntity.ok(ApiResponse.success("Thứ hạng hiện tại", rank));
    }
}
