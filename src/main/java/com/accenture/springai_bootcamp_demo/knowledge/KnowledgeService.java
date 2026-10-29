package com.accenture.springai_bootcamp_demo.knowledge;

import com.accenture.springai_bootcamp_demo.knowledge.KnowledgeDtos.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Small local lexical retriever; no embedding service or vector database needed. */
@Service
@RequiredArgsConstructor
public class KnowledgeService {
    private static final int CHUNK_SIZE = 900;
    private static final int OVERLAP = 100;
    private static final Pattern WORD = Pattern.compile("[\\p{L}\\p{N}]+");
    private static final Set<String> STOP_WORDS = Set.of("a", "an", "the", "is", "are", "was", "were",
            "what", "how", "why", "when", "where", "who", "which", "do", "does", "did", "to", "of",
            "and", "or", "in", "on", "for", "with", "it", "this", "that", "be", "can", "i", "my",
            "me", "about", "please", "explain", "give", "using");
    private final KnowledgeRepository repository;

    @Transactional
    public DocumentDto create(CreateDocumentRequest request) {
        return toDto(repository.save(KnowledgeDocument.create(request.title(), request.content())));
    }

    @Transactional(readOnly = true)
    public List<DocumentDto> list() {
        return repository.findAllByOrderByCreatedAtAscIdAsc().stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public DocumentDto get(String id) {
        return toDto(repository.findById(id).orElseThrow(() -> new KnowledgeNotFoundException(id)));
    }

    @Transactional
    public void delete(String id) {
        var document = repository.findById(id).orElseThrow(() -> new KnowledgeNotFoundException(id));
        repository.delete(document);
    }

    @Transactional(readOnly = true)
    public List<Source> search(String query, int limit) {
        Set<String> terms = tokens(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        var matches = new ArrayList<Source>();
        for (var document : repository.findAllByOrderByCreatedAtAscIdAsc()) {
            var titleTerms = tokens(document.getTitle());
            String text = document.getContent();
            for (int start = 0, chunk = 1; start < text.length(); start += CHUNK_SIZE - OVERLAP, chunk++) {
                int end = Math.min(start + CHUNK_SIZE, text.length());
                String content = text.substring(start, end);
                var contentTerms = tokens(content);
                long contentHits = terms.stream().filter(contentTerms::contains).count();
                if (contentHits > 0) {
                    long titleHits = terms.stream().filter(titleTerms::contains).count();
                    double score = (contentHits + 2.0 * titleHits) / Math.sqrt(Math.max(1, contentTerms.size()));
                    matches.add(new Source(document.getId() + ":" + chunk, document.getId(),
                            document.getTitle(), content, score));
                }
                if (end == text.length()) {
                    break;
                }
            }
        }
        return matches.stream().sorted(Comparator.comparingDouble(Source::score).reversed()
                .thenComparing(Source::id)).limit(limit).toList();
    }

    private Set<String> tokens(String text) {
        return WORD.matcher(text.toLowerCase(Locale.ROOT)).results().map(match -> match.group())
                .filter(word -> word.length() > 1 && !STOP_WORDS.contains(word)).collect(Collectors.toSet());
    }

    private DocumentDto toDto(KnowledgeDocument document) {
        return new DocumentDto(document.getId(), document.getTitle(), document.getContent(), document.getCreatedAt());
    }
}
