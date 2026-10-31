package backend.hoctap.service.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
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
 *                       AI_MODEL=gemini-2.5-flash          AI_API_KEY=<key aistudio.google.com>
 *   OpenAI:             AI_BASE_URL=https://api.openai.com/v1
 *                       AI_MODEL=gpt-4o-mini               AI_API_KEY=sk-...
 *   Ollama (máy-local): AI_BASE_URL=http://localhost:11434/v1
 *                       AI_MODEL=qwen3.8                   AI_API_KEY=ollama
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

    @Value("${ai.model:gemini-2.5-flash}")
    private String model;

    /** Số token tối đa cho một lượt sinh — reasoning model (qwen3...) cần dư cho phần suy nghĩ */
    @Value("${ai.max-tokens:8192}")
    private int maxTokens;

    /** Gửi 1 lượt chat, trả về nội dung text của câu trả lời */
    public String chat(String systemPrompt, String userPrompt) {
        String key = apiKey != null ? apiKey.trim() : "";
        if (key.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Tính năng AI chưa được cấu hình. Vui lòng đặt biến môi trường AI_API_KEY "
                    + "(hướng dẫn trong README-DEPLOY.md — Gemini miễn phí hoặc Ollama chạy tại máy).");
        }

        // Timeout dài: LLM sinh văn bản có thể mất vài phút (đặc biệt model chạy CPU)
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(15));
        factory.setReadTimeout(Duration.ofMinutes(15));

        RestClient client = RestClient.builder()
                .baseUrl(stripTrailingSlash(baseUrl))
                .requestFactory(factory)
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + key)
                .build();

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.2,
                "max_tokens", maxTokens,
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
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            log.error("AI chat call failed: {}", e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Không gọi được AI (" + e.getMessage() + "). Kiểm tra AI_BASE_URL / AI_API_KEY / AI_MODEL.");
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
                // Một số provider (reasoning model) trả nội dung rỗng khi hết token ở phần suy luận
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
