package com.accenture.springai_bootcamp_demo.client;

import com.accenture.springai_bootcamp_demo.config.AiProperties;
import com.accenture.springai_bootcamp_demo.entity.ChatMessage;
import java.util.List;
import java.util.function.Supplier;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Shared Spring AI gateway for chat and role-specific local agents. */
@Component
@Slf4j
public class OllamaChatClient {
    private final ChatClient client;
    private final String systemPrompt;

    public OllamaChatClient(ChatClient.Builder builder, AiProperties properties) {
        client = builder.build();
        systemPrompt = properties.systemPrompt();
    }

    public String complete(List<ChatMessage> history) {
        return invoke(() -> client.prompt().system(systemPrompt)
                .messages(ConversationMessages.from(history)).call().content());
    }

    public String generate(String system, String userPrompt) {
        return invoke(() -> client.prompt().system(system)
                .messages(new UserMessage(userPrompt)).call().content());
    }

    private String invoke(Supplier<String> operation) {
        try {
            String content = operation.get();
            if (!StringUtils.hasText(content)) {
                throw new AiModelException("Ollama returned an empty response. Try the request again.");
            }
            return content.trim();
        } catch (AiModelException ex) {
            throw ex;
        } catch (RuntimeException ex) {
            log.warn("Local Ollama generation failed: {}", ex.toString());
            throw new AiModelException("Local model request failed. Check that Ollama is running and the configured model is pulled.", ex);
        }
    }
}
