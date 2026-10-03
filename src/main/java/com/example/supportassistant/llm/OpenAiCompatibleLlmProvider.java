package com.example.supportassistant.llm;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.example.supportassistant.domain.Ticket;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(name = "app.llm.provider", havingValue = "openai")
public class OpenAiCompatibleLlmProvider implements LlmProvider {

    private final RestClient client;
    private final ObjectMapper objectMapper;
    private final String endpoint;
    private final String model;
    private final String apiKey;
    private final int maxAttempts;

    public OpenAiCompatibleLlmProvider(
            RestClient client,
            ObjectMapper objectMapper,
            @Value("${app.llm.endpoint}") String endpoint,
            @Value("${app.llm.model}") String model,
            @Value("${app.llm.api-key:}") String apiKey,
            @Value("${app.llm.max-attempts:3}") int maxAttempts) {

        this.client = client;
        this.objectMapper = objectMapper;
        this.endpoint = endpoint;
        this.model = model;
        this.apiKey = apiKey;
        this.maxAttempts = Math.max(1, maxAttempts);
    }

    @Override
    public LlmAnalysis analyze(Ticket ticket) {

        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmProviderException(
                    "LLM API key is not configured"
            );
        }

        if (endpoint == null || endpoint.isBlank()) {
            throw new LlmProviderException(
                    "LLM endpoint is not configured"
            );
        }

        if (model == null || model.isBlank()) {
            throw new LlmProviderException(
                    "LLM model is not configured"
            );
        }

        ChatRequest request = new ChatRequest(
                model,
                List.of(
                        new Message(
                                "system",
                                PromptTemplate.SYSTEM_PROMPT
                        ),
                        new Message(
                                "user",
                                PromptTemplate.userPrompt(ticket)
                        )
                ),
                0.1,
                new ResponseFormat("json_object")
        );

        Exception lastFailure = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {

            try {
                return callProvider(request);

            } catch (InvalidLlmResponseException e) {

                // The provider responded, but its output was invalid.
                // Retrying will not normally fix malformed structured output.
                throw e;

            } catch (LlmProviderException e) {

                lastFailure = e;

                if (attempt < maxAttempts) {
                    sleepBackoff(attempt);
                }

            } catch (Exception e) {

                lastFailure = e;

                if (attempt < maxAttempts) {
                    sleepBackoff(attempt);
                }
            }
        }

        String reason = lastFailure == null
                ? "unknown error"
                : lastFailure.getMessage();

        throw new LlmProviderException(
                "LLM provider failed after "
                        + maxAttempts
                        + " attempts. Reason: "
                        + reason,
                lastFailure
        );
    }

    private LlmAnalysis callProvider(ChatRequest request) {

        try {

            JsonNode response = client.post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .headers(headers -> headers.setBearerAuth(apiKey))
                    .body(request)
                    .retrieve()
                    .onStatus(
                            status -> status.isError(),
                            (httpRequest, httpResponse) -> {

                                String body = new String(
                                        httpResponse
                                                .getBody()
                                                .readAllBytes(),
                                        StandardCharsets.UTF_8
                                );

                                String safeBody = truncate(body, 1500);

                                throw new LlmProviderException(
                                        "LLM API returned HTTP "
                                                + httpResponse
                                                        .getStatusCode()
                                                        .value()
                                                + ": "
                                                + safeBody
                                );
                            }
                    )
                    .body(JsonNode.class);

            if (response == null) {
                throw new InvalidLlmResponseException(
                        "LLM returned an empty response"
                );
            }

            /*
             * Expected OpenAI-compatible response:
             *
             * {
             *   "choices": [
             *     {
             *       "message": {
             *         "content": "{...}"
             *       }
             *     }
             *   ]
             * }
             */

            String content = response
                    .path("choices")
                    .path(0)
                    .path("message")
                    .path("content")
                    .asText(null);

            if (content == null || content.isBlank()) {
                throw new InvalidLlmResponseException(
                        "LLM response did not contain message content"
                );
            }

            String json = extractJsonObject(content);

            try {

                LlmAnalysis analysis =
                        objectMapper.readValue(
                                json,
                                LlmAnalysis.class
                        );

                validate(analysis);

                return analysis;

            } catch (InvalidLlmResponseException e) {

                throw e;

            } catch (Exception e) {

                throw new InvalidLlmResponseException(
                        "LLM returned invalid structured JSON: "
                                + e.getMessage()
                );
            }

        } catch (ResourceAccessException e) {

            throw new LlmProviderException(
                    "Unable to connect to LLM provider: "
                            + e.getMessage(),
                    e
            );

        } catch (LlmProviderException e) {

            /*
             * Preserve the original HTTP status/error.
             */
            throw e;

        } catch (Exception e) {

            throw new LlmProviderException(
                    "Unexpected LLM provider error: "
                            + e.getMessage(),
                    e
            );
        }
    }

    private static String extractJsonObject(String content) {

        String trimmed = content.trim();

        /*
         * Handles responses such as:
         *
         * {
         *   "category": "...",
         *   ...
         * }
         *
         * and also responses accidentally wrapped in
         * markdown/code fences.
         */

        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');

        if (start < 0 || end <= start) {
            throw new InvalidLlmResponseException(
                    "LLM response is not a JSON object"
            );
        }

        return trimmed.substring(start, end + 1);
    }

    private static void validate(LlmAnalysis analysis) {

        if (analysis == null) {
            throw new InvalidLlmResponseException(
                    "LLM output is null"
            );
        }

        if (blank(analysis.category())) {
            throw new InvalidLlmResponseException(
                    "LLM output category is missing"
            );
        }

        if (blank(analysis.summary())) {
            throw new InvalidLlmResponseException(
                    "LLM output summary is missing"
            );
        }

        if (blank(analysis.suggestedResponse())) {
            throw new InvalidLlmResponseException(
                    "LLM output suggestedResponse is missing"
            );
        }

        if (blank(analysis.recommendedTeam())) {
            throw new InvalidLlmResponseException(
                    "LLM output recommendedTeam is missing"
            );
        }

        if (Double.isNaN(analysis.confidence())
                || Double.isInfinite(analysis.confidence())) {

            throw new InvalidLlmResponseException(
                    "LLM output confidence is invalid"
            );
        }

        if (analysis.confidence() < 0.0
                || analysis.confidence() > 1.0) {

            throw new InvalidLlmResponseException(
                    "LLM output confidence must be between 0 and 1"
            );
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String truncate(String value, int maxLength) {

        if (value == null) {
            return "";
        }

        if (value.length() <= maxLength) {
            return value;
        }

        return value.substring(0, maxLength)
                + "...";
    }

    private static void sleepBackoff(int attempt) {

        try {

            Thread.sleep(500L * attempt);

        } catch (InterruptedException e) {

            Thread.currentThread().interrupt();

            throw new LlmProviderException(
                    "LLM retry interrupted",
                    e
            );
        }
    }

    private record ChatRequest(
            String model,
            List<Message> messages,
            double temperature,
            ResponseFormat response_format) {
    }

    private record Message(
            String role,
            String content) {
    }

    private record ResponseFormat(
            String type) {
    }
}