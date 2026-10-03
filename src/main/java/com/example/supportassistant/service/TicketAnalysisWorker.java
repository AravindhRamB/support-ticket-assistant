package com.example.supportassistant.service;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.example.supportassistant.domain.Ticket;
import com.example.supportassistant.llm.InvalidLlmResponseException;
import com.example.supportassistant.llm.LlmAnalysis;
import com.example.supportassistant.llm.LlmProvider;

@Service
public class TicketAnalysisWorker {

    private static final Logger log =
            LoggerFactory.getLogger(TicketAnalysisWorker.class);

    private final TicketAnalysisPersistenceService persistenceService;
    private final LlmProvider llmProvider;

    public TicketAnalysisWorker(
            TicketAnalysisPersistenceService persistenceService,
            LlmProvider llmProvider) {

        this.persistenceService = persistenceService;
        this.llmProvider = llmProvider;
    }

    @Async("llmTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handle(TicketService.TicketCreated event) {
        analyze(event.ticketId());
    }

    public void analyze(UUID ticketId) {

        try {

            Ticket ticket =
                    persistenceService.markProcessing(ticketId);

            if (ticket == null) {
                log.warn(
                        "Ticket disappeared before analysis. ticketId={}",
                        ticketId
                );
                return;
            }

            LlmAnalysis result =
                    llmProvider.analyze(ticket);

            validate(result);

            persistenceService.completeAnalysis(
                    ticketId,
                    result
            );

            log.info(
                    "Ticket analysis completed. ticketId={}",
                    ticketId
            );

        } catch (Exception exception) {

            String safeMessage =
                    failureMessage(exception);

            try {

                persistenceService.markFailed(
                        ticketId,
                        safeMessage
                );

            } catch (Exception persistenceException) {

                log.error(
                        "Failed to mark ticket as FAILED. ticketId={}",
                        ticketId,
                        persistenceException
                );
            }

            log.warn(
                    "Ticket analysis failed. ticketId={} reason={}",
                    ticketId,
                    safeMessage
            );
        }
    }

    static void validate(LlmAnalysis result) {

        if (result == null
                || blank(result.category())
                || blank(result.summary())
                || blank(result.suggestedResponse())
                || blank(result.recommendedTeam())
                || Double.isNaN(result.confidence())
                || Double.isInfinite(result.confidence())
                || result.confidence() < 0.0
                || result.confidence() > 1.0) {

            throw new InvalidLlmResponseException(
                    "LLM output failed validation"
            );
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String failureMessage(Exception exception) {

        if (exception instanceof InvalidLlmResponseException) {
            return exception.getMessage();
        }

        String message = exception.getMessage();

        return message == null || message.isBlank()
                ? "LLM analysis failed"
                : message;
    }
}