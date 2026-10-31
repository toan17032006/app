package backend.hoctap.filter;

import backend.hoctap.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    JWT AUTH FILTER                          ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * OncePerRequestFilter → Đảm bảo filter chỉ chạy 1 lần mỗi request.
 * (Spring có thể gọi filter nhiều lần trong một request — class này ngăn điều đó)
 *
 * Luồng hoạt động:
 *   Request đến
 *     ↓
 *   Filter đọc header: "Authorization: Bearer <token>"
 *     ↓
 *   Nếu không có token → bỏ qua (request tiếp tục, SecurityConfig sẽ chặn nếu cần)
 *     ↓
 *   Nếu có token → verify → set Authentication vào SecurityContext
 *     ↓
 *   Controller nhận request (đã được xác thực)
 *
 * SecurityContext là "phòng lưu trữ" của Spring Security cho mỗi request.
 * Sau khi set Authentication vào đây, Spring Security coi request là đã xác thực.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        // Bước 1: Lấy Authorization header
        // Header chuẩn: "Authorization: Bearer eyJ..."
        final String authHeader = request.getHeader("Authorization");

        // Bước 2: Kiểm tra có token không
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            // Không có token → cho qua (SecurityConfig sẽ quyết định chặn hay không)
            filterChain.doFilter(request, response);
            return;
        }

        // Bước 3: Cắt "Bearer " (7 ký tự) để lấy token thuần
        final String token = authHeader.substring(7);

        try {
            // Bước 4: Đọc email từ token
            final String email = jwtUtil.extractEmail(token);
            final Long userId  = jwtUtil.extractUserId(token);

            // Bước 5: Chỉ xử lý nếu chưa có Authentication trong context
            // (tránh xử lý 2 lần)
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {

                // Bước 6: Xác thực token
                if (jwtUtil.isTokenValid(token, email)) {

                    // Bước 7: Tạo Authentication object và đưa vào SecurityContext
                    // List.of() → empty authorities (chưa có role/permission)
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(
                                    email,      // principal = email
                                    null,       // credentials = null (không cần password nữa)
                                    List.of()   // authorities = [] (thêm roles sau)
                            );

                    // Đính kèm thông tin request (IP, session...)
                    authToken.setDetails(
                            new WebAuthenticationDetailsSource().buildDetails(request));

                    // Đặt vào SecurityContext → request được coi là đã xác thực
                    SecurityContextHolder.getContext().setAuthentication(authToken);

                    // Lưu userId vào request attribute để Controller dùng
                    request.setAttribute("userId", userId);
                }
            }
        } catch (Exception e) {
            // Token lỗi → không set Authentication → SecurityConfig sẽ trả 401
            // Không ném exception ở đây để filterChain vẫn tiếp tục
        }

        // Bước 8: Chuyển request cho filter/controller tiếp theo
        filterChain.doFilter(request, response);
    }
}
