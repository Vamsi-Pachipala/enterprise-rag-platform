package com.vamsi.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record DocumentRequest(
        @NotBlank(message = "Document text cannot be empty")
        String text
) {}