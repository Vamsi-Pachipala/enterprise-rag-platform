package com.vamsi.rag.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class VectorStoreService {

    private final VectorStore vectorStore;

    public VectorStoreService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void addDocument(String text) {

        Document document = new Document(
                text,
                Map.of("source", "manual")
        );

        vectorStore.add(List.of(document));
    }

    public List<Document> searchDocuments(String query) {

        SearchRequest searchRequest = SearchRequest.builder()
                .query(query)
                .topK(3)
                .build();

        return vectorStore.similaritySearch(searchRequest);
    }

    public void deleteBySource(String filename) {

        Filter.Expression filter =
                new Filter.Expression(
                        Filter.ExpressionType.EQ,
                        new Filter.Key("source"),
                        new Filter.Value(filename)
                );

        vectorStore.delete(filter);
    }
}