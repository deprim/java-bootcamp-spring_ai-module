package com.accenture.springai_bootcamp_demo;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ChatApiTests {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final List<JsonNode> REQUESTS = new CopyOnWriteArrayList<>();
    private static volatile String reply = "A local model reply.";
    private static volatile int status = 200;
    private static final HttpServer OLLAMA = startOllama();

    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws IOException {
        var database = Files.createTempFile("bootcamp-chat-test-", ".db");
        database.toFile().deleteOnExit();
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + database);
        registry.add("spring.ai.ollama.base-url", () -> "http://localhost:" + OLLAMA.getAddress().getPort());
    }

    @BeforeEach
    void reset() {
        REQUESTS.clear();
        reply = "A local model reply.";
        status = 200;
    }

    @Test
    void persistsExchangesAndSendsOrderedHistoryToLocalModel() throws Exception {
        String id = createChat();
        mvc.perform(post("/api/chats/{id}/messages", id).contentType("application/json")
                        .content("{\"content\":\"Explain Java records\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chatMessages[1].content").value(reply));
        mvc.perform(post("/api/chats/{id}/chatMessages", id).contentType("application/json")
                        .content("{\"content\":\"Give an example\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.chatMessages.length()").value(4));
        assertThat(REQUESTS).hasSize(2);
        JsonNode messages = REQUESTS.getLast().get("messages");
        assertThat(messages.get(0).get("role").asText()).isEqualTo("system");
        assertThat(messages.get(1).get("content").asText()).isEqualTo("Explain Java records");
        assertThat(messages.get(2).get("role").asText()).isEqualTo("assistant");
        assertThat(messages.get(3).get("content").asText()).isEqualTo("Give an example");
        mvc.perform(get("/api/chats/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Explain Java records"))
                .andExpect(jsonPath("$.chatMessages[0].role").value("USER"))
                .andExpect(jsonPath("$.chatMessages[1].role").value("ASSISTANT"))
                .andExpect(jsonPath("$.chatMessages[2].role").value("USER"));
        mvc.perform(get("/api/chats")).andExpect(status().isOk());
        mvc.perform(delete("/api/chats/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/chats/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void modelFailureRollsBackMessagesAndTitle() throws Exception {
        String id = createChat();
        status = 500;
        mvc.perform(post("/api/chats/{id}/messages", id).contentType("application/json")
                        .content("{\"content\":\"Do not save a partial exchange\"}"))
                .andExpect(status().isBadGateway()).andExpect(jsonPath("$.detail").isNotEmpty());
        mvc.perform(get("/api/chats/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.chatMessages").isEmpty())
                .andExpect(jsonPath("$.title").value("New chat"));
    }

    @Test
    void emptyModelReplyIsAnUpstreamError() throws Exception {
        String id = createChat();
        reply = "   ";
        mvc.perform(post("/api/chats/{id}/messages", id).contentType("application/json")
                        .content("{\"content\":\"Hello\"}"))
                .andExpect(status().isBadGateway());
        mvc.perform(get("/api/chats/{id}", id)).andExpect(jsonPath("$.chatMessages").isEmpty());
    }

    @Test
    void rejectsInvalidAndUnknownChatRequestsWithoutCallingModel() throws Exception {
        String id = createChat();
        for (String content : List.of("", " ", "x".repeat(8001))) {
            mvc.perform(post("/api/chats/{id}/messages", id).contentType("application/json")
                            .content(JSON.writeValueAsString(java.util.Map.of("content", content))))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/chats/missing/messages").contentType("application/json")
                        .content("{\"content\":\"Hello\"}"))
                .andExpect(status().isNotFound());
        assertThat(REQUESTS).isEmpty();
    }

    private String createChat() throws Exception {
        var result = mvc.perform(post("/api/chats")).andExpect(status().isCreated()).andReturn();
        return JSON.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    @Test
    void tutorEndpointsReturnSourcesAndThreeAgentStages() throws Exception {
        mvc.perform(post("/api/tutor/ask").contentType("application/json")
                        .content("{\"question\":\"What is constructor injection?\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.answer").value(reply))
                .andExpect(jsonPath("$.sources[0].id").isNotEmpty());
        REQUESTS.clear();
        mvc.perform(post("/api/tutor/study-plan").contentType("application/json")
                        .content("{\"topic\":\"Java records\",\"level\":\"BEGINNER\",\"days\":2}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.plan").value(reply))
                .andExpect(jsonPath("$.stages[0].agent").value("researcher"))
                .andExpect(jsonPath("$.stages[1].agent").value("tutor"))
                .andExpect(jsonPath("$.stages[2].agent").value("reviewer"));
        assertThat(REQUESTS).hasSize(3);
    }

    @Test
    void tutorRejectsBlankTopicsUnknownLevelsAndOutOfRangeDays() throws Exception {
        for (String payload : List.of(
                "{\"topic\":\" \",\"level\":\"BEGINNER\",\"days\":2}",
                "{\"topic\":\"Java\",\"level\":\"BEGINNER\",\"days\":0}",
                "{\"topic\":\"Java\",\"level\":\"BEGINNER\",\"days\":8}",
                "{\"topic\":\"Java\",\"level\":\"EXPERT\",\"days\":2}",
                "{\"topic\":\"Java\",\"days\":2}")) {
            mvc.perform(post("/api/tutor/study-plan").contentType("application/json").content(payload))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/tutor/ask").contentType("application/json").content("{\"question\":\" \"}"))
                .andExpect(status().isBadRequest());
        assertThat(REQUESTS).isEmpty();
    }

    private static HttpServer startOllama() {
        try {
            var server = HttpServer.create(new java.net.InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/api/chat", exchange -> {
                REQUESTS.add(JSON.readTree(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                String response = status == 200
                        ? JSON.writeValueAsString(java.util.Map.of("model", "gemma3:1b", "created_at",
                        "2026-10-29T12:00:00Z", "message", java.util.Map.of("role", "assistant", "content", reply),
                        "done", true, "done_reason", "stop", "eval_count", 5, "prompt_eval_count", 10))
                        : "{\"error\":\"Model is unavailable\"}";
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().set("Content-Type", "application/json");
                exchange.sendResponseHeaders(status, bytes.length);
                exchange.getResponseBody().write(bytes);
                exchange.close();
            });
            server.start();
            Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
            return server;
        } catch (IOException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
