package com.vamsi.rag.controller;

import com.vamsi.rag.dto.EmbeddingRequest;
import com.vamsi.rag.dto.EmbeddingResponse;
import com.vamsi.rag.service.EmbeddingService;

import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/embeddings")
public class EmbeddingController {

    private final EmbeddingService embeddingService;

    public EmbeddingController(EmbeddingService embeddingService) {
        this.embeddingService = embeddingService;
    }

    @PostMapping
    public EmbeddingResponse generate(
            @Valid @RequestBody EmbeddingRequest request) {

        float[] embedding =
                embeddingService.generateEmbedding(request.text());

        return new EmbeddingResponse(
                embedding.length,
                embedding
        );
    }
}