package com.vamsi.rag.controller;

import com.vamsi.rag.dto.DocumentRequest;
import com.vamsi.rag.dto.SearchRequest;
import com.vamsi.rag.dto.SearchResponse;
import com.vamsi.rag.service.VectorStoreService;
import jakarta.validation.Valid;
import org.springframework.ai.document.Document;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private final VectorStoreService vectorStoreService;

    public DocumentController(VectorStoreService vectorStoreService) {
        this.vectorStoreService = vectorStoreService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String addDocument(@Valid @RequestBody DocumentRequest request) {

        vectorStoreService.addDocument(request.text());

        return "Document stored successfully";
    }

    @PostMapping("/search")
    public SearchResponse search(@Valid @RequestBody SearchRequest request) {

        List<Document> documents =
                vectorStoreService.searchDocuments(request.query());

        List<String> results = documents.stream()
                .map(Document::getText)
                .toList();

        return new SearchResponse(results);
    }

    @DeleteMapping("/by-source")
    public ResponseEntity<String> deleteBySource(
            @RequestParam String filename) {

        vectorStoreService.deleteBySource(filename);

        return ResponseEntity.ok(
                "Deleted documents for: " + filename
        );
    }
}