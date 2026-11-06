package com.vamsi.rag.dto;

public record EmbeddingResponse(
        int dimensions,
        float[] embedding
) {}