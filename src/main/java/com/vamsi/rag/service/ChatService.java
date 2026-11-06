package com.vamsi.rag.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;

    public ChatService(ChatClient.Builder builder) {
        this.chatClient = builder.build();
    }

    public String generateResponse(String question) {
        return chatClient.prompt()
                .user(question)
                .call()
                .content();
    }
}