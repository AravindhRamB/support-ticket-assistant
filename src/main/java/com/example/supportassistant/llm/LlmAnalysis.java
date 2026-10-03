package com.example.supportassistant.llm;

public record LlmAnalysis(
        String category,
        String summary,
        String suggestedResponse,
        String recommendedTeam,
        double confidence
) {}
