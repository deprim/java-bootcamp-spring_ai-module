package com.accenture.springai_bootcamp_demo.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public final class KnowledgeDtos {
    private KnowledgeDtos() { }

    public record CreateDocumentRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank @Size(max = 20000) String content) { }

    public record DocumentDto(String id, String title, String content, Instant createdAt) { }

    public record Source(String id, String documentId, String title, String content, double score) { }
}
