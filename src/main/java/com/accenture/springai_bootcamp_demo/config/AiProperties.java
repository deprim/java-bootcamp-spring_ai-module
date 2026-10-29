package com.accenture.springai_bootcamp_demo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "ai")
public record AiProperties(String systemPrompt) {
    public AiProperties {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            systemPrompt = "You are a helpful, concise Java bootcamp tutor. Answer in English.";
        }
    }
}
