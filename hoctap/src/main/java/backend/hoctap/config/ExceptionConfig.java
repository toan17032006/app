package backend.hoctap.config;

import backend.hoctap.dto.response.common.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import java.io.IOException;
import java.util.List;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║              GLOBAL EXCEPTION HANDLER                       ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Tất cả exception đều được bắt ở đây → trả về JSON chuẩn ApiResponse.
 * Không bao giờ trả response rỗng hoặc HTML → tránh lỗi JSON parse ở frontend.
 */
@Configuration
public class ExceptionConfig {

    private static final ObjectMapper mapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    /**
     * Khi chưa đăng nhập mà truy cập endpoint cần auth:
     * Spring Security mặc định trả 401 với body rỗng → lỗi JSON parse.
     * Bean này thay đổi thành trả JSON chuẩn.
     */
    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, authException) -> {
            writeJsonError(response, HttpServletResponse.SC_UNAUTHORIZED,
                    "Bạn chưa đăng nhập hoặc token đã hết hạn");
        };
    }

    /**
     * Khi đã đăng nhập nhưng không có quyền (403):
     */
    @Bean
    public AccessDeniedHandler accessDeniedHandler() {
        return (request, response, accessDeniedException) -> {
            writeJsonError(response, HttpServletResponse.SC_FORBIDDEN,
                    "Bạn không có quyền truy cập tài nguyên này");
        };
    }

    private static void writeJsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        ApiResponse<?> body = ApiResponse.error(message);
        response.getWriter().write(mapper.writeValueAsString(body));
    }

    /**
     * Xử lý tất cả exception từ Controller/Service → trả JSON.
     */
    @RestControllerAdvice
    public static class GlobalExceptionHandler {

        /** ResponseStatusException — ném từ service */
        @ExceptionHandler(ResponseStatusException.class)
        public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleStatus(ResponseStatusException ex) {
            return org.springframework.http.ResponseEntity
                    .status(ex.getStatusCode())
                    .body(ApiResponse.error(ex.getReason()));
        }

        /** Validation errors — @Valid thất bại */
        @ExceptionHandler(MethodArgumentNotValidException.class)
        public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
            List<String> errors = ex.getBindingResult().getFieldErrors().stream()
                    .map(f -> f.getField() + ": " + f.getDefaultMessage())
                    .toList();

            return org.springframework.http.ResponseEntity
                    .badRequest()
                    .body(ApiResponse.error("Dữ liệu không hợp lệ", errors));
        }

        /** Mọi exception khác */
        @ExceptionHandler(Exception.class)
        public org.springframework.http.ResponseEntity<ApiResponse<Void>> handleAll(Exception ex) {
            return org.springframework.http.ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Lỗi hệ thống: " + ex.getMessage()));
        }
    }
}
