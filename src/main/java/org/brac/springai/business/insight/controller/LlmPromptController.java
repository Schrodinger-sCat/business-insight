package org.brac.springai.business.insight.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/llm")
public class LlmPromptController {
    ChatClient chatClient;
    private final VectorStore vectorStore;

    public LlmPromptController(OllamaChatModel ollamaChatModel, VectorStore vectorStore) {
        this.chatClient = ChatClient.create(ollamaChatModel);
        this.vectorStore = vectorStore;
    }

    @GetMapping("/test")
    public ResponseEntity test() {
        return ResponseEntity.ok(
                chatClient
                        .prompt("What is the capital of Bangladesh?")
                        .call()
                        .content()
        );
    }

    @PostMapping("/rag")
    public ResponseEntity<String> rag(@RequestParam String query,
                                      @RequestParam(defaultValue = "3") int topK) {
        if (!StringUtils.hasText(query)) {
            return ResponseEntity.badRequest().body("Query cannot be empty");
        }

        // Step 1: Search relevant documents from the vector store
        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(topK)
                .build();
        List<Document> docs = vectorStore.similaritySearch(searchRequest);

        // Step 2: Concatenate retrieved documents as context
        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        // Step 3: Ask the LLM with context (using ChatClient like in /test)
        String response = chatClient
                .prompt("Use the following context to answer the question:\n\n" + context +
                        "\n\nQuestion: " + query)
                .call()
                .content();

        return ResponseEntity.ok(response);
    }
}
