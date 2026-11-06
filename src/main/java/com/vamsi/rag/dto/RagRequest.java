package com.vamsi.rag.dto;

import jakarta.validation.constraints.NotBlank;

public record RagRequest(
        @NotBlank(message = "Question cannot be empty")
        String question
) {}