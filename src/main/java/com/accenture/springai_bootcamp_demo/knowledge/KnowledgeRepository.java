package com.accenture.springai_bootcamp_demo.knowledge;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KnowledgeRepository extends JpaRepository<KnowledgeDocument, String> {
    List<KnowledgeDocument> findAllByOrderByCreatedAtAscIdAsc();
}
