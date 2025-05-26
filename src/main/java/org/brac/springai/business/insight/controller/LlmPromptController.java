package org.brac.springai.business.insight.controller;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class LlmPromptController {
    ChatClient chatClient;

    public LlmPromptController(OllamaChatModel ollamaChatModel) {
        this.chatClient = ChatClient.create(ollamaChatModel);
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
}
