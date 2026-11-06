package com.vamsi.rag.controller;

import com.vamsi.rag.dto.RagRequest;
import com.vamsi.rag.dto.RagResponse;
import com.vamsi.rag.service.RagService;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rag")
public class RagController {

    private final RagService ragService;

    public RagController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    public ResponseEntity<RagResponse> ask(
            @Valid @RequestBody RagRequest request) {

        return ResponseEntity.ok(
                ragService.askQuestion(request.question())
        );
    }
}