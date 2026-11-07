
package com.vamsi.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class FileIngestionService {

    private final VectorStore vectorStore;
    private final StructureAwareChunker chunker;

    public FileIngestionService(
            VectorStore vectorStore,
            StructureAwareChunker chunker) {

        this.vectorStore = vectorStore;
        this.chunker = chunker;
    }

    public int ingest(MultipartFile file) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Uploaded file cannot be empty");
        }

        String filename = file.getOriginalFilename();

        if (filename == null || filename.isBlank()) {
            throw new IllegalArgumentException(
                    "Filename is required");
        }

        // 1. Extract documents
        List<Document> documents;

        if (filename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {

            PagePdfDocumentReader reader =
                    new PagePdfDocumentReader(
                            file.getResource(),
                            PdfDocumentReaderConfig.builder()
                                    .withPagesPerDocument(1)
                                    .build()
                    );

            documents = reader.get();

        } else {

            TikaDocumentReader reader =
                    new TikaDocumentReader(file.getResource());

            documents = reader.get();
        }

        // 2. Validate extracted content
        if (documents == null ||
                documents.isEmpty() ||
                documents.stream().allMatch(document ->
                        document.getText() == null ||
                                document.getText().isBlank())) {

            throw new IllegalArgumentException(
                    "No readable text found in uploaded file");
        }

        // 3. Structure-aware chunking
        List<Document> chunks = chunker.chunk(documents);

        if (chunks.isEmpty()) {
            throw new IllegalArgumentException(
                    "No usable chunks generated");
        }

        // 4. Preserve metadata and add file information
        List<Document> enrichedChunks = chunks.stream()
                .map(chunk -> {

                    Map<String, Object> metadata =
                            new HashMap<>(chunk.getMetadata());

                    metadata.put("source", filename);

                    metadata.put(
                            "fileType",
                            file.getContentType() == null
                                    ? "unknown"
                                    : file.getContentType()
                    );

                    return new Document(
                            chunk.getText(),
                            metadata
                    );
                })
                .toList();

        // 5. Store chunks in Qdrant
        vectorStore.add(enrichedChunks);

        return enrichedChunks.size();
    }
}
