package com.accenture.springai_bootcamp_demo.knowledge;

import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Seeds bundled course notes on an empty database, never duplicating existing notes. */
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "knowledge.seed-enabled", havingValue = "true", matchIfMissing = true)
public class KnowledgeSeed implements ApplicationRunner {
    private final KnowledgeRepository repository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) throws Exception {
        if (repository.count() > 0) {
            return;
        }
        for (String name : List.of("java-basics", "spring-boot", "jpa-transactions", "ollama-spring-ai")) {
            String content = new ClassPathResource("knowledge/" + name + ".md").getContentAsString(StandardCharsets.UTF_8);
            String title = content.lines().findFirst().orElse(name).replaceFirst("^#\\s*", "");
            repository.save(KnowledgeDocument.create(title, content));
        }
    }
}
