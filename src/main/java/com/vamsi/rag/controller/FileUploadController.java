package com.vamsi.rag.controller;

import com.vamsi.rag.service.FileIngestionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
public class FileUploadController {

    private final FileIngestionService ingestionService;

    public FileUploadController(FileIngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String, Object>> upload(
            @RequestParam("file") MultipartFile file)
            throws IOException {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "File cannot be empty"));
        }

        int chunks = ingestionService.ingest(file);

        return ResponseEntity.ok(Map.of(
                "message", "Document uploaded successfully",
                "filename", file.getOriginalFilename(),
                "chunksStored", chunks
        ));
    }
}