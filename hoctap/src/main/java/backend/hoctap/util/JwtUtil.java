package backend.hoctap.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                       JWT UTILITY                           ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * @Component → Giống @Service nhưng dùng cho class "tiện ích".
 * Spring vẫn tạo và quản lý instance, nhưng ngữ nghĩa khác:
 *   @Service   → Tầng nghiệp vụ
 *   @Component → Class hỗ trợ, tiện ích
 *
 * JWT có 3 phần:
 *   HEADER   → Thuật toán ký (HS256, RS256...)
 *   PAYLOAD  → Thông tin user (claims): userId, email, exp...
 *   SIGNATURE→ Chữ ký = HMAC(header + payload, secretKey)
 *
 * Khi verify: server ký lại header+payload → so với signature
 * → Khớp → token hợp lệ, không ai giả mạo payload.
 *
 * @Value("${jwt.secret}") → Đọc giá trị từ application.properties
 * Thay vì hardcode trong code → dễ đổi theo môi trường (dev/prod).
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;           // milliseconds

    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;    // milliseconds

    // ─────────────────────────────────────────────────────────────────────────
    // TẠO TOKEN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Tạo ACCESS TOKEN cho user.
     *
     * Claims là payload của JWT — bạn đưa thông tin gì vào,
     * client sẽ đọc được khi decode token (không cần gọi API).
     * Nhưng đừng đưa thông tin nhạy cảm (mật khẩu, số thẻ...) vào đây!
     */
    public String generateAccessToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("email", email);
        claims.put("type", "access");

        return buildToken(claims, email, expiration);
    }

    /**
     * Tạo REFRESH TOKEN.
     *
     * Refresh token tồn tại lâu hơn (7 ngày).
     * Khi access token hết hạn (24h), client dùng refresh token
     * để lấy access token mới — không cần đăng nhập lại.
     */
    public String generateRefreshToken(Long userId, String email) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "refresh");

        return buildToken(claims, email, refreshExpiration);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // ĐỌC THÔNG TIN TỪ TOKEN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Lấy email (subject) từ token.
     * Subject là field tiêu chuẩn của JWT — thường chứa ID hoặc email.
     */
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }

    /** Lấy userId từ claims trong payload */
    public Long extractUserId(String token) {
        Object userId = extractAllClaims(token).get("userId");
        if (userId instanceof Integer) return ((Integer) userId).longValue();
        if (userId instanceof Long)    return (Long) userId;
        return null;
    }

    /** Kiểm tra loại token: access hay refresh */
    public String extractType(String token) {
        return (String) extractAllClaims(token).get("type");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // XÁC THỰC TOKEN
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Token hợp lệ khi: chữ ký đúng + chưa hết hạn.
     *
     * Tại sao truyền email vào?
     * → Để đảm bảo token được cấp cho đúng người.
     *   Nếu token hợp lệ nhưng email khác → từ chối.
     */
    public boolean isTokenValid(String token, String email) {
        try {
            String tokenEmail = extractEmail(token);
            return tokenEmail.equals(email) && !isTokenExpired(token);
        } catch (JwtException e) {
            return false; // Token bị giả mạo, sai format, hoặc hết hạn
        }
    }

    public boolean isTokenExpired(String token) {
        return extractAllClaims(token).getExpiration().before(new Date());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE HELPER
    // ─────────────────────────────────────────────────────────────────────────

    private String buildToken(Map<String, Object> claims, String subject, long ttl) {
        Date now    = new Date();
        Date expire = new Date(now.getTime() + ttl);

        return Jwts.builder()
                .claims(claims)             // Payload: thông tin bổ sung
                .subject(subject)           // subject = email
                .issuedAt(now)              // iat: thời điểm tạo
                .expiration(expire)         // exp: thời điểm hết hạn
                .signWith(getSignKey())     // Ký bằng HMAC-SHA256
                .compact();                 // Build thành chuỗi JWT
    }

    /**
     * Giải mã và xác thực chữ ký token.
     * Ném JwtException nếu token sai/hết hạn.
     */
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * Tạo SecretKey từ chuỗi secret trong application.properties.
     * Keys.hmacShaKeyFor() → tạo key HMAC-SHA256 từ byte array.
     */
    private SecretKey getSignKey() {
        byte[] keyBytes = secret.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
