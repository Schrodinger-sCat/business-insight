package org.brac.springai.business.insight.controller;

import org.brac.springai.business.insight.requestBody.UpsertRequest;
import org.brac.springai.business.insight.service.PdfVectorService;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chroma")
public class ChromaVectorDbController {

    private final VectorStore vectorStore;

    private final PdfVectorService pdfVectorService;


    public ChromaVectorDbController(VectorStore vectorStore, PdfVectorService pdfVectorService) {
        this.vectorStore = vectorStore;
        this.pdfVectorService = pdfVectorService;
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

        vectorStore.add(List.of(doc)); // Embeds and stores automatically
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

    @PostMapping("/upsert/pdf")
    public ResponseEntity<List<String>> upsertPdf(@RequestParam("file") MultipartFile file) {
        try {
            List<String> docIds = pdfVectorService.upsertPdf(file);
            return ResponseEntity.ok(docIds);
        } catch (Exception e) {
            List<String> list = new ArrayList<>();
            list.add(e.getMessage());
            return ResponseEntity.status(500).body(list);
        }
    }
}
