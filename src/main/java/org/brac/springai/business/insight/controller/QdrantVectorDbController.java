package org.brac.springai.business.insight.controller;


import org.brac.springai.business.insight.requestBody.UpsertRequest;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/qdrant")
public class QdrantVectorDbController {

    private final VectorStore vectorStore;

    public QdrantVectorDbController(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }


    @PostMapping("/upsert/text")
    public ResponseEntity<String> upsertText(@RequestParam String text) {
        if (!StringUtils.hasText(text)) {
            return ResponseEntity.badRequest().body("Text cannot be empty");
        }

        Document doc = Document.builder()
                .text(text)
                .metadata("source", "manual-upsert")
                .build();

        vectorStore.add(List.of(doc)); // Automatically embeds and stores the doc
        return ResponseEntity.ok("Inserted document with ID: " + doc.getId());
    }


    @PostMapping("/upsert/doc")
    public ResponseEntity<String> upsertDocument(@RequestBody UpsertRequest request) {
        if (!StringUtils.hasText(request.text)) {
            return ResponseEntity.badRequest().body("Text cannot be empty");
        }

        Document doc = Document.builder()
                .text(request.text)
                .metadata(request.metadata != null ? request.metadata : Map.of())
                .build();

        vectorStore.add(List.of(doc));
        return ResponseEntity.ok("Inserted document with ID: " + doc.getId());
    }


    @GetMapping("/search")
    public ResponseEntity<List<Document>> searchSimilar(
            @RequestParam String query,
            @RequestParam(defaultValue = "3") int topK,
            @RequestParam(required = false) Double threshold,
            @RequestParam(required = false) String filterExpression
    ) {
        var builder = SearchRequest.builder()
                .query(query)
                .topK(topK);

        if (threshold != null) {
            builder.similarityThreshold(threshold);
        }

        if (filterExpression != null && !filterExpression.isBlank()) {
            builder.filterExpression(filterExpression);
        }

        SearchRequest request = builder.build();
        return ResponseEntity.ok(vectorStore.similaritySearch(request));
    }
}