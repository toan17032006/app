package backend.hoctap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tắt toàn bộ Spring AI autoconfigure vì project này chưa dùng AI.
 * Nếu sau này muốn bật lại: xóa dòng exclude và thêm API key vào application.properties.
 *
 * Cách tắt trong code này đáng tin cậy hơn application.properties
 * vì nó dùng tên class thật, không phụ thuộc vào tên property của từng module.
 */
@SpringBootApplication(exclude = {
        // Tắt DeepSeek AI
        org.springframework.ai.model.deepseek.autoconfigure.DeepSeekChatAutoConfiguration.class,
        // Tắt Google GenAI
        org.springframework.ai.model.google.genai.autoconfigure.chat.GoogleGenAiChatAutoConfiguration.class,
        // Tắt Ollama AI
        org.springframework.ai.model.ollama.autoconfigure.OllamaChatAutoConfiguration.class
})
public class HoctapApplication {

    public static void main(String[] args) {
        SpringApplication.run(HoctapApplication.class, args);
    }

}
