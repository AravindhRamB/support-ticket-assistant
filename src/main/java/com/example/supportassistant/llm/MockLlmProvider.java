package com.example.supportassistant.llm;

import com.example.supportassistant.domain.Ticket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "mock", matchIfMissing = true)
public class MockLlmProvider implements LlmProvider {

    @Override
    public LlmAnalysis analyze(Ticket ticket) {
        String text = (ticket.getSubject() + " " + ticket.getDescription()).toLowerCase();

        if (text.contains("import") || text.contains("upload") || text.contains("document")) {
            return new LlmAnalysis(
                    "IMPORT_FAILURE",
                    "The customer is experiencing a document import or upload issue.",
                    "Thank you for reporting the issue. We are investigating the import problem and will follow up with the next steps.",
                    "IMPORT_ENGINEERING",
                    0.90
            );
        }

        if (text.contains("login") || text.contains("password") || text.contains("sign in") || text.contains("authentication")) {
            return new LlmAnalysis(
                    "AUTHENTICATION",
                    "The customer is reporting an authentication or sign-in issue.",
                    "Thank you for contacting support. We are reviewing the sign-in issue and will help restore access.",
                    "IDENTITY_SUPPORT",
                    0.88
            );
        }

        if (text.contains("payment") || text.contains("billing") || text.contains("invoice")) {
            return new LlmAnalysis(
                    "BILLING",
                    "The customer is reporting a billing or payment-related issue.",
                    "Thank you for contacting support. We are reviewing the billing issue and will follow up with the next steps.",
                    "BILLING_SUPPORT",
                    0.86
            );
        }

        return new LlmAnalysis(
                "GENERAL_SUPPORT",
                "The customer has reported a support issue requiring investigation.",
                "Thank you for contacting support. We have received your request and are reviewing the issue.",
                "GENERAL_SUPPORT",
                0.72
        );
    }
}
