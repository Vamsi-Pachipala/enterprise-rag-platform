package com.vamsi.rag.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

@Component
public class CalculatorTools {

    @Tool(description = "Multiply two numbers and return the result")
    public double multiply(
            @ToolParam(description = "First number") double a,
            @ToolParam(description = "Second number") double b) {
        System.out.println("Calculating: " + a + " * " + b);
        return a * b;
    }
}