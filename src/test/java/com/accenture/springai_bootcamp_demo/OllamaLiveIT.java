package com.accenture.springai_bootcamp_demo;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/** Explicit acceptance test: mvn -Pollama-live verify (requires real Ollama). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OllamaLiveIT {
    @LocalServerPort int port;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final JsonMapper json = JsonMapper.builder().build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        var database = Files.createTempFile("bootcamp-live-", ".db");
        database.toFile().deleteOnExit();
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + database);
    }

    @Test
    void realLocalModelAnswersChatGroundedQuestionAndThreeAgentStudyPlan() throws Exception {
        String chat = post("/api/chats", Map.of("title", "Local acceptance test"), 201).get("id").asText();
        JsonNode exchange = post("/api/chats/" + chat + "/messages",
                Map.of("content", "In one English sentence, explain what a Java record is."), 200);
        assertThat(exchange.get("chatMessages").size()).isEqualTo(2);
        assertThat(exchange.get("chatMessages").get(1).get("content").asText()).isNotBlank();

        JsonNode document = post("/api/knowledge/documents", Map.of("title", "Workshop room",
                "content", "The workshop room code is ORBIT-42. Participants meet in that room."), 201);
        JsonNode answer = post("/api/tutor/ask", Map.of("question", "What is the workshop room code?"), 200);
        assertThat(answer.get("answer").asText()).contains("ORBIT-42");
        assertThat(answer.get("sources").get(0).get("documentId").asText()).isEqualTo(document.get("id").asText());

        JsonNode plan = post("/api/tutor/study-plan", Map.of("topic", "Java records", "level", "BEGINNER", "days", 2), 200);
        assertThat(plan.get("plan").asText()).isNotBlank();
        assertThat(plan.get("stages").size()).isEqualTo(3);
        assertThat(plan.get("stages").get(0).get("agent").asText()).isEqualTo("researcher");
        assertThat(plan.get("stages").get(1).get("agent").asText()).isEqualTo("tutor");
        assertThat(plan.get("stages").get(2).get("agent").asText()).isEqualTo("reviewer");
        for (JsonNode stage : plan.get("stages")) {
            assertThat(stage.get("output").asText()).isNotBlank();
        }
        assertThat(plan.get("plan").asText()).isEqualTo(plan.get("stages").get(2).get("output").asText());
        assertThat(plan.get("sources").isEmpty()).isFalse();
    }

    private JsonNode post(String path, Map<String, ?> payload, int expectedStatus) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                .timeout(Duration.ofSeconds(600)).header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json.writeValueAsString(payload))).build();
        var response = http.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).withFailMessage(response.body()).isEqualTo(expectedStatus);
        return json.readTree(response.body());
    }
}
