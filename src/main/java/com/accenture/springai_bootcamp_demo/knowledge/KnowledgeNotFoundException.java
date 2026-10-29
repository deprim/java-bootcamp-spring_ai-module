package com.accenture.springai_bootcamp_demo.knowledge;

public class KnowledgeNotFoundException extends RuntimeException {
    public KnowledgeNotFoundException(String id) {
        super("Knowledge document not found: " + id);
    }
}
