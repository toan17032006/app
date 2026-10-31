package backend.hoctap.config;

import backend.hoctap.filter.JwtAuthFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                    SECURITY CONFIG                          ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * @Configuration → Class này chứa các @Bean definition.
 *   Spring đọc class này khi khởi động và đăng ký các Bean.
 *
 * @EnableWebSecurity → Bật Spring Security cho ứng dụng.
 *
 * Các khái niệm quan trọng:
 *
 * 1. SecurityFilterChain → Chuỗi filter, quyết định ai được vào đâu.
 *
 * 2. CSRF (Cross-Site Request Forgery):
 *    → Tấn công giả mạo request từ browser của victim.
 *    → REST API dùng JWT → KHÔNG cần CSRF protection.
 *    → Chỉ cần CSRF cho form-based login với session/cookie.
 *
 * 3. Session:
 *    → STATELESS: Server không lưu session. Mỗi request tự mang JWT.
 *    → Ngược lại với web truyền thống dùng HttpSession.
 *
 * 4. CORS (Cross-Origin Resource Sharing):
 *    → Frontend (localhost:3000) gọi API backend (localhost:8080) = khác origin.
 *    → Browser chặn cross-origin request theo mặc định.
 *    → Phải cấu hình CORS để cho phép frontend gọi được.
 *
 * 5. BCrypt → Thuật toán hash mật khẩu mạnh nhất hiện tại.
 *    → strength=10 → hash 2^10 = 1024 rounds → chậm cố ý để chống brute force.
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final org.springframework.security.web.AuthenticationEntryPoint authenticationEntryPoint;
    private final org.springframework.security.web.access.AccessDeniedHandler accessDeniedHandler;

    /** Danh sách origin cho phép gọi API — cấu hình qua biến môi trường CORS_ALLOWED_ORIGINS */
    @org.springframework.beans.factory.annotation.Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:3001}")
    private String allowedOrigins;

    /**
     * Bean chính — định nghĩa luật bảo mật HTTP.
     *
     * Các endpoint PUBLIC (không cần token):
     *   POST /api/auth/**  → Đăng ký, đăng nhập
     *   GET  /api/exams    → Xem danh sách bài thi
     *   GET  /api/leaderboard → Xem bảng xếp hạng
     *
     * Mọi endpoint khác → CẦN JWT token hợp lệ.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 1. Tắt CSRF (REST API + JWT → không cần)
            .csrf(AbstractHttpConfigurer::disable)

            // 2. Cấu hình CORS (cho phép frontend Next.js gọi)
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))

            // 3. Định nghĩa quyền truy cập từng endpoint
            .authorizeHttpRequests(auth -> auth
                // Public endpoints — không cần token
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/exams").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/leaderboard/**").permitAll()

                // File bài tập học sinh nộp (được nhúng qua thẻ <img>/<a> — không mang JWT)
                .requestMatchers("/uploads/**").permitAll()

                // Swagger UI (nếu cần xem API docs khi dev)
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()

                // Trang static của frontend (khi đóng gói chung 1 JAR)
                // Lưu ý: SpaForwardController forward /login → /login.html — Spring Security
                // lọc cả FORWARD dispatch nên phải permit cả đuôi .html
                .requestMatchers("/", "/index.html", "/login", "/register",
                                 "/forgot-password", "/reset-password", "/terms", "/privacy",
                                 "/*.html", "/*.js", "/*.css",
                                 "/*.png", "/*.jpg", "/*.svg", "/*.ico", "/*.txt", "/*.json",
                                 "/_next/**").permitAll()

                // Tất cả endpoint còn lại → phải có JWT
                .anyRequest().authenticated()
            )

            // 4. Khi 401/403 → trả JSON (không phải body rỗng)
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint(authenticationEntryPoint)
                .accessDeniedHandler(accessDeniedHandler)
            )

            // 5. Stateless session — không lưu state trên server
            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

            // 6. Thêm JwtAuthFilter vào trước filter xác thực mặc định
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * BCrypt PasswordEncoder.
     *
     * Cách dùng:
     *   encoder.encode("rawPassword")         → "$2a$10$..."
     *   encoder.matches("raw", "$2a$10$...")  → true/false
     *
     * Mỗi lần encode ra chuỗi khác nhau (do random salt)
     * nhưng matches() vẫn đúng → không thể reverse engineer.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    /**
     * AuthenticationManager → Spring dùng nội bộ để xác thực.
     * Cần expose Bean này để AuthService có thể inject và dùng.
     */
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    /**
     * CORS Configuration:
     *   allowedOrigins  → Đọc từ app.cors.allowed-origins (biến môi trường CORS_ALLOWED_ORIGINS)
     *                     phân cách bằng dấu phẩy. Giá trị "*" = cho phép mọi origin (dev/demo).
     *   allowedMethods  → Các HTTP method được phép
     *   allowedHeaders  → Cho phép header Authorization (JWT) và Content-Type
     *   allowCredentials→ Cho phép gửi cookie/credentials
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);

        List<String> origins = java.util.Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        if (origins.contains("*")) {
            // Khi cho phép mọi origin thì phải dùng pattern, không được set cả allowCredentials=true
            config.setAllowedOriginPatterns(List.of("*"));
            config.setAllowCredentials(false);
        } else {
            config.setAllowedOrigins(origins);
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config); // Áp dụng cho tất cả path
        return source;
    }
}
