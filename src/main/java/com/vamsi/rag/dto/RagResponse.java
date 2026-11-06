package com.vamsi.rag.dto;

import java.util.List;

public record RagResponse(
        String answer,
        List<SourceReference> sources
) {}