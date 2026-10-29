package com.accenture.springai_bootcamp_demo.knowledge;

import com.accenture.springai_bootcamp_demo.knowledge.KnowledgeDtos.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.net.URI;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/knowledge")
@RequiredArgsConstructor
public class KnowledgeController {
    private final KnowledgeService knowledge;

    @GetMapping("/documents")
    public List<DocumentDto> list() {
        return knowledge.list();
    }

    @GetMapping("/documents/{id}")
    public DocumentDto get(@PathVariable String id) {
        return knowledge.get(id);
    }

    @PostMapping("/documents")
    public ResponseEntity<DocumentDto> create(@Valid @RequestBody CreateDocumentRequest request) {
        var document = knowledge.create(request);
        return ResponseEntity.created(URI.create("/api/knowledge/documents/" + document.id())).body(document);
    }

    @DeleteMapping("/documents/{id}")
    public ResponseEntity<Void> delete(@PathVariable String id) {
        knowledge.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/search")
    public List<Source> search(@RequestParam @NotBlank @Size(max = 2000) String query,
                               @RequestParam(defaultValue = "3") @Min(1) @Max(5) int limit) {
        return knowledge.search(query, limit);
    }
}
