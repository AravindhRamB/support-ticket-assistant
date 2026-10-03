package com.example.supportassistant.llm;

public class InvalidLlmResponseException extends RuntimeException {
    public InvalidLlmResponseException(String message) { super(message); }
}
