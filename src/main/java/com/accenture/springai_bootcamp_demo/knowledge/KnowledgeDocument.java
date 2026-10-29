package com.accenture.springai_bootcamp_demo.knowledge;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "knowledge_documents")
@Getter
@NoArgsConstructor
public class KnowledgeDocument {
    @Id
    private String id;
    @Column(nullable = false, length = 200)
    private String title;
    @Column(nullable = false, length = 20000)
    private String content;
    @Column(nullable = false)
    private Instant createdAt;

    public static KnowledgeDocument create(String title, String content) {
        var document = new KnowledgeDocument();
        document.id = UUID.randomUUID().toString();
        document.title = title.trim();
        document.content = content.trim();
        document.createdAt = Instant.now();
        return document;
    }
}
