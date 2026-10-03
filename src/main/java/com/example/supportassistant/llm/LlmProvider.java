package com.example.supportassistant.llm;

import com.example.supportassistant.domain.Ticket;

public interface LlmProvider {
    LlmAnalysis analyze(Ticket ticket);
}
