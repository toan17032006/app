package backend.hoctap.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ╔══════════════════════════════════════════════════════════════╗
 * ║                AI CHAT CLIENT (OpenAI-compatible)           ║
 * ╚══════════════════════════════════════════════════════════════╝
 *
 * Gọi API chat theo chuẩn OpenAI (POST {base-url}/chat/completions) —
 * nhờ đó dùng được với hầu hết nhà cung cấp chỉ bằng biến môi trường:
 *
 *   Gemini (miễn phí):  AI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai
 *                       AI_MODEL=gemini-flash-latest        AI_API_KEY=<key aistudio.google.com>
 *   OpenAI:             AI_BASE_URL=https://api.openai.com/v1
 *                       AI_MODEL=gpt-4o-mini                AI_API_KEY=sk-...
 *   Ollama (máy-local): AI_BASE_URL=http://localhost:11434/v1
 *                       AI_MODEL=qwen3:4b                   AI_API_KEY=ollama
 *
 * Chống quá tải (bản miễn phí Gemini hay trả 503 "high demand"):
 *   1. Tự THỬ LẠI model chính vài lần với khoảng nghỉ tăng dần
 *   2. Vẫn lỗi → tự chuyển sang các model DỰ PHÒNG (AI_FALLBACK_MODELS,
 *      mặc định: gemini-flash-lite-latest, gemini-2.5-flash-lite — ít quá tải hơn)
 *
 * Không cấu hình AI_API_KEY → các tính năng AI trả lỗi rõ ràng cho người dùng,
 * phần còn lại của app vẫn hoạt động bình thường.
 */
@Component
@Slf4j
public class AiChatClient {

    @Value("${ai.base-url:https://generativelanguage.googleapis.com/v1beta/openai}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:gemini-flash-latest}")
    private String model;

    /** Model dự phòng, phân cách bằng dấu phẩy — dùng khi model chính lỗi 5xx/429/404 */
    @Value("${ai.fallback-models:gemini-flash-lite-latest,gemini-2.5-flash-lite}")
    private String fallbackModels;

    /** Số lần thử lại thêm cho MỖI model khi gặp lỗi tạm thời (503/429/500) */
    @Value("${ai.retry-attempts:2}")
    private int retryAttempts;

    /** Số token tối đa cho một lượt sinh — reasoning model (qwen3...) cần dư cho phần suy nghĩ */
    @Value("${ai.max-tokens:8192}")
    private int maxTokens;

    /** Gửi 1 lượt chat với max_tokens mặc định */
    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, null);
    }

    /**
     * Gửi 1 lượt chat, trả về nội dung text của câu trả lời.
     * @param maxTokensOverride nâng trần token khi cần output dài (vd 100 câu hỏi)
     */
    public String chat(String systemPrompt, String userPrompt, Integer maxTokensOverride) {
        String key = apiKey != null ? apiKey.trim() : "";
        if (key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Tính năng AI chưa được cấu hình. Vui lòng đặt biến môi trường AI_API_KEY "
                    + "(hướng dẫn trong README-DEPLOY.md — Gemini miễn phí hoặc Ollama chạy tại máy).");
        }

        RestClient client = buildClient(key);

        // Chuỗi model để thử: model chính trước, sau đó các model dự phòng
        List<String> models = new ArrayList<>();
        models.add(model.trim());
        for (String m : fallbackModels.split(",")) {
            if (!m.isBlank()) models.add(m.trim());
        }

        ResponseStatusException lastError = null;
        for (String currentModel : models) {
            for (int attempt = 0; attempt <= Math.max(0, retryAttempts); attempt++) {
                try {
                    String content = doChat(client, currentModel, systemPrompt, userPrompt, maxTokensOverride);
                    if (!currentModel.equals(model.trim())) {
                        log.info("AI succeeded with fallback model '{}'", currentModel);
                    }
                    return content;
                } catch (ResponseStatusException e) {
                    lastError = e;
                    // 400/401/403 = cấu hình/key sai → đổi model cũng vô ích, dừng luôn
                    if (e.getStatusCode().value() == 400 || e.getStatusCode().value() == 401
                            || e.getStatusCode().value() == 403) {
                        throw e;
                    }
                    log.warn("AI call failed (model={}, attempt {}/{}): {}",
                            currentModel, attempt + 1, retryAttempts + 1, shortMessage(e));
                    if (attempt < retryAttempts) sleepQuietly(5L * (attempt + 1)); // nghỉ 5s rồi 10s
                }
            }
            log.info("Switching to fallback AI model after '{}' failed", currentModel);
        }

        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "AI đang quá tải hoặc không khả dụng (đã thử các model: "
                + String.join(", ", models) + "). Chi tiết: " + shortMessage(lastError)
                + ". Vui lòng thử lại sau vài phút.");
    }

    /** Một lượt gọi API với 1 model cụ thể */
    private String doChat(RestClient client, String currentModel,
                          String systemPrompt, String userPrompt, Integer maxTokensOverride) {
        int tokens = maxTokensOverride != null ? maxTokensOverride : maxTokens;
        Map<String, Object> body = Map.of(
                "model", currentModel,
                "temperature", 0.2,
                "max_tokens", tokens,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        try {
            // Đọc dạng String rồi tự parse — không phụ thuộc Content-Type mà provider trả về
            String responseBody = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);

            return extractContent(responseBody);
        } catch (RestClientResponseException e) {
            // Lỗi HTTP từ nhà cung cấp (4xx/5xx) — giữ nguyên status + rút gọn message
            throw new ResponseStatusException(
                    HttpStatus.resolve(e.getStatusCode().value()) == null
                            ? HttpStatus.BAD_GATEWAY
                            : HttpStatus.valueOf(e.getStatusCode().value()),
                    shortMessage(new ResponseStatusException(
                            HttpStatus.BAD_GATEWAY, e.getResponseBodyAsString())));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI chat call failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không gọi được AI (" + e.getMessage() + "). Kiểm tra AI_BASE_URL / AI_API_KEY / AI_MODEL.");
        }
    }

    private RestClient buildClient(String key) {
        // Timeout dài: LLM sinh văn bản có thể mất vài phút (đặc biệt model chạy CPU)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofMinutes(15));

        return RestClient.builder()
                .baseUrl(stripTrailingSlash(baseUrl))
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .build();
    }

    /** Rút gọn thông báo lỗi JSON của nhà cung cấp cho người dùng đọc được */
    private String shortMessage(ResponseStatusException e) {
        String msg = e.getReason() != null ? e.getReason() : (e.getMessage() == null ? "unknown" : e.getMessage());
        msg = msg.replace("\n", " ").replace("\r", "").replaceAll("\\s+", " ").trim();
        return msg.length() > 220 ? msg.substring(0, 220) + "..." : msg;
    }

    private void sleepQuietly(long seconds) {
        try {
            Thread.sleep(seconds * 1000L);
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
        }
    }

    /** Lấy choices[0].message.content từ JSON phản hồi chuẩn OpenAI-compatible */
    private String extractContent(String responseBody) {
        try {
            com.fasterxml.jackson.databind.JsonNode root =
                    new com.fasterxml.jackson.databind.ObjectMapper().readTree(responseBody);
            com.fasterxml.jackson.databind.JsonNode content = root
                    .path("choices").path(0).path("message").path("content");
            if (content.isMissingNode() || content.asText("").isBlank()) {
                // Một số provider (reasoning model) trả nội dung rỗng khi hết token cho phần suy luận
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                        "AI trả về nội dung rỗng (model có thể đã dùng hết token cho phần suy luận). "
                        + "Thử lại hoặc tăng AI_MAX_TOKENS.");
            }
            return content.asText();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "AI trả về phản hồi không đúng định dạng: " + e.getMessage());
        }
    }

    private String stripTrailingSlash(String url) {
        String s = url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
        // Java resolve "localhost" sang IPv6 (::1) trước trên Windows trong khi
        // Ollama chỉ nghe IPv4 → ép dùng 127.0.0.1 cho địa chỉ local
        return s.replaceFirst("(?<=//)localhost(?=:)", "127.0.0.1");
    }
}
