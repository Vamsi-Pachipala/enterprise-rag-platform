package com.vamsi.rag.controller;

import com.vamsi.rag.dto.ChatRequest;
import com.vamsi.rag.dto.ChatResponse;
import com.vamsi.rag.service.ChatService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostMapping
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {

        String answer = chatService.generateResponse(request.question());

        return new ChatResponse(answer);
    }
}