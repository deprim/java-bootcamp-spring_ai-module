package com.accenture.springai_bootcamp_demo.client;

import com.accenture.springai_bootcamp_demo.entity.ChatMessage;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

/** Converts persisted history without flattening away speaker roles. */
public final class ConversationMessages {
    private ConversationMessages() { }

    public static List<Message> from(List<ChatMessage> history) {
        return history.stream().map(ConversationMessages::from).toList();
    }

    private static Message from(ChatMessage message) {
        return switch (message.getRole()) {
            case SYSTEM -> new SystemMessage(message.getContent());
            case USER -> new UserMessage(message.getContent());
            case ASSISTANT -> new AssistantMessage(message.getContent());
        };
    }
}
