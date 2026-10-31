package backend.hoctap.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Điều hướng trang frontend khi app được đóng gói THÀNH 1 JAR DUY NHẤT:
 * frontend (Next.js static export) nằm trong classpath:/static.
 *
 * `next build` với `output: 'export'` sinh ra: /index.html, /login.html,
 * /register.html, /forgot-password.html... — URL không có đuôi .html
 * nên cần forward qua đây để người dùng mở /login mà không gặp 404.
 *
 * Khi chạy dev (frontend ở port 3000 gọi API 8081) các trang này không
 * bao giờ được request tới backend nên không ảnh hưởng gì.
 */
@Controller
public class SpaForwardController {

    @GetMapping({"/", "/login", "/register",
                 "/forgot-password", "/reset-password", "/terms", "/privacy"})
    public String forwardSpaPage(HttpServletRequest request) {
        String path = request.getRequestURI();
        String page = path.equals("/") ? "index" : path.substring(1);
        return "forward:/" + page + ".html";
    }
}
