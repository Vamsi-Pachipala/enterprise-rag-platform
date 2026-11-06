package com.vamsi.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record SearchRequest(
        @NotBlank(message = "Search query cannot be empty")
        String query
) {}