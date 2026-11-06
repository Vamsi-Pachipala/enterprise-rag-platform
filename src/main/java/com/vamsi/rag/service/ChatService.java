package com.vamsi.rag.service;

import com.vamsi.rag.tools.CalculatorTools;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final CalculatorTools calculatorTools;

    public ChatService(
            ChatClient.Builder chatClientBuilder,
            CalculatorTools calculatorTools) {

        this.chatClient = chatClientBuilder.build();
        this.calculatorTools = calculatorTools;
    }

    public String generateResponse(String question) {
        return chatClient.prompt()
                .tools(calculatorTools)
                .user(question)
                .call()
                .content();
    }
}