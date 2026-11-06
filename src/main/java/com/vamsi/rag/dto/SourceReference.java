package com.vamsi.rag.dto;

public record SourceReference(
        String fileName,
        Integer pageNumber,
        String content
) {}