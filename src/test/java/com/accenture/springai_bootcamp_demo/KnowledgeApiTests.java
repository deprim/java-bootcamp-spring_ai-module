package com.accenture.springai_bootcamp_demo;

import java.nio.file.Files;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "knowledge.seed-enabled=false")
@AutoConfigureMockMvc
class KnowledgeApiTests {
    @Autowired MockMvc mvc;
    private final JsonMapper json = JsonMapper.builder().build();

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) throws Exception {
        var database = Files.createTempFile("bootcamp-knowledge-test-", ".db");
        database.toFile().deleteOnExit();
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + database);
    }

    @Test
    void notesAreSearchableAndDeletionRemovesTheirEvidence() throws Exception {
        String id = add("Quasar lifecycle", "Quasar services are injected through constructor injection.");
        mvc.perform(get("/api/knowledge/documents")).andExpect(status().isOk());
        mvc.perform(get("/api/knowledge/search").param("query", "How does quasar injection work?").param("limit", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].documentId").value(id))
                .andExpect(jsonPath("$[0].content").value("Quasar services are injected through constructor injection."));
        mvc.perform(delete("/api/knowledge/documents/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/knowledge/search").param("query", "quasar"))
                .andExpect(status().isOk()).andExpect(content().json("[]"));
        mvc.perform(delete("/api/knowledge/documents/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void longDocumentsYieldBoundedRelevantChunksAndDeterministicRanking() throws Exception {
        String id = add("Zephyr transactions", "unrelated ".repeat(170) + "Zephyr rollback protects atomicity. ".repeat(30));
        String response = mvc.perform(get("/api/knowledge/search").param("query", "zephyr rollback").param("limit", "2"))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].documentId").value(id))
                .andReturn().getResponse().getContentAsString();
        var sources = json.readTree(response);
        assertThat(sources.size()).isBetween(1, 2);
        assertThat(sources.get(0).get("content").asText()).hasSizeLessThanOrEqualTo(900).contains("rollback");
        mvc.perform(get("/api/knowledge/search").param("query", "zephyr rollback").param("limit", "2"))
                .andExpect(content().json(response));
    }

    @Test
    void noMatchesAndPunctuationDoNotFabricateEvidence() throws Exception {
        for (String query : new String[]{"!!??", "unfindablexzpq", "what is the"}) {
            mvc.perform(get("/api/knowledge/search").param("query", query))
                    .andExpect(status().isOk()).andExpect(content().json("[]"));
        }
    }

    @Test
    void validatesNotesAndSearchBounds() throws Exception {
        mvc.perform(post("/api/knowledge/documents").contentType("application/json")
                        .content("{\"title\":\" \",\"content\":\"\"}"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/knowledge/search").param("query", "java").param("limit", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/knowledge/search").param("query", " "))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createdDocumentCanBeReadAtItsLocation() throws Exception {
        var created = mvc.perform(post("/api/knowledge/documents").contentType("application/json")
                        .content("{\"title\":\"Readable note\",\"content\":\"A persisted course note.\"}"))
                .andExpect(status().isCreated()).andReturn();
        String location = created.getResponse().getHeader("Location");
        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Readable note"))
                .andExpect(jsonPath("$.content").value("A persisted course note."));
        mvc.perform(get("/api/knowledge/documents/missing")).andExpect(status().isNotFound());
    }

    private String add(String title, String content) throws Exception {
        String response = mvc.perform(post("/api/knowledge/documents").contentType("application/json")
                        .content(json.writeValueAsString(java.util.Map.of("title", title, "content", content))))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return json.readTree(response).get("id").asText();
    }
}
