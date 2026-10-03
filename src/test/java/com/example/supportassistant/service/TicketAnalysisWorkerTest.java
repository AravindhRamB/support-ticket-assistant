package com.example.supportassistant.service;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.supportassistant.domain.AnalysisStatus;
import com.example.supportassistant.domain.Priority;
import com.example.supportassistant.domain.Ticket;
import com.example.supportassistant.llm.LlmAnalysis;
import com.example.supportassistant.llm.LlmProvider;

class TicketAnalysisWorkerTest {

    @Test
    void shouldMarkFailedWhenProviderThrows() {

        var persistenceService =
                mock(TicketAnalysisPersistenceService.class);

        var provider =
                mock(LlmProvider.class);

        var worker = new TicketAnalysisWorker(
                persistenceService,
                provider
        );

        UUID ticketId = UUID.randomUUID();

        Ticket ticket = ticket();

        when(persistenceService.markProcessing(ticketId))
                .thenReturn(ticket);

        when(provider.analyze(ticket))
                .thenThrow(new RuntimeException("timeout"));

        worker.analyze(ticketId);

        verify(persistenceService)
                .markProcessing(ticketId);

        verify(provider)
                .analyze(ticket);

        verify(persistenceService)
                .markFailed(
                        eq(ticketId),
                        eq("timeout")
                );

        verify(persistenceService, never())
                .completeAnalysis(any(), any());
    }

    @Test
    void shouldMarkFailedWhenLlmOutputIsInvalid() {

        var persistenceService =
                mock(TicketAnalysisPersistenceService.class);

        var provider =
                mock(LlmProvider.class);

        var worker = new TicketAnalysisWorker(
                persistenceService,
                provider
        );

        UUID ticketId = UUID.randomUUID();

        Ticket ticket = ticket();

        when(persistenceService.markProcessing(ticketId))
                .thenReturn(ticket);

        when(provider.analyze(ticket))
                .thenReturn(
                        new LlmAnalysis(
                                "",
                                "",
                                "",
                                "",
                                2.0
                        )
                );

        worker.analyze(ticketId);

        verify(persistenceService)
                .markProcessing(ticketId);

        verify(provider)
                .analyze(ticket);

        verify(persistenceService)
                .markFailed(
                        eq(ticketId),
                        eq("LLM output failed validation")
                );

        verify(persistenceService, never())
                .completeAnalysis(any(), any());
    }

    @Test
    void shouldCompleteWhenLlmOutputIsValid() {

        var persistenceService =
                mock(TicketAnalysisPersistenceService.class);

        var provider =
                mock(LlmProvider.class);

        var worker = new TicketAnalysisWorker(
                persistenceService,
                provider
        );

        UUID ticketId = UUID.randomUUID();

        Ticket ticket = ticket();

        LlmAnalysis analysis =
                new LlmAnalysis(
                        "IMPORT_FAILURE",
                        "Import is failing.",
                        "We are investigating the import issue.",
                        "IMPORT_ENGINEERING",
                        0.91
                );

        when(persistenceService.markProcessing(ticketId))
                .thenReturn(ticket);

        when(provider.analyze(ticket))
                .thenReturn(analysis);

        worker.analyze(ticketId);

        verify(persistenceService)
                .markProcessing(ticketId);

        verify(provider)
                .analyze(ticket);

        verify(persistenceService)
                .completeAnalysis(
                        ticketId,
                        analysis
                );

        verify(persistenceService, never())
                .markFailed(any(), anyString());
    }

    @Test
    void shouldDoNothingWhenTicketDoesNotExist() {

        var persistenceService =
                mock(TicketAnalysisPersistenceService.class);

        var provider =
                mock(LlmProvider.class);

        var worker = new TicketAnalysisWorker(
                persistenceService,
                provider
        );

        UUID ticketId = UUID.randomUUID();

        when(persistenceService.markProcessing(ticketId))
                .thenReturn(null);

        worker.analyze(ticketId);

        verify(persistenceService)
                .markProcessing(ticketId);

        verify(provider, never())
                .analyze(any());

        verify(persistenceService, never())
                .completeAnalysis(any(), any());

        verify(persistenceService, never())
                .markFailed(any(), anyString());
    }

    @Test
    void shouldMarkFailedWhenLlmReturnsNull() {

        var persistenceService =
                mock(TicketAnalysisPersistenceService.class);

        var provider =
                mock(LlmProvider.class);

        var worker = new TicketAnalysisWorker(
                persistenceService,
                provider
        );

        UUID ticketId = UUID.randomUUID();

        Ticket ticket = ticket();

        when(persistenceService.markProcessing(ticketId))
                .thenReturn(ticket);

        when(provider.analyze(ticket))
                .thenReturn(null);

        worker.analyze(ticketId);

        verify(persistenceService)
                .markProcessing(ticketId);

        verify(provider)
                .analyze(ticket);

        verify(persistenceService)
                .markFailed(
                        eq(ticketId),
                        eq("LLM output failed validation")
                );

        verify(persistenceService, never())
                .completeAnalysis(any(), any());
    }

    @Test
    void shouldMarkFailedWhenConfidenceIsOutsideValidRange() {

        var persistenceService =
                mock(TicketAnalysisPersistenceService.class);

        var provider =
                mock(LlmProvider.class);

        var worker = new TicketAnalysisWorker(
                persistenceService,
                provider
        );

        UUID ticketId = UUID.randomUUID();

        Ticket ticket = ticket();

        LlmAnalysis invalidAnalysis =
                new LlmAnalysis(
                        "IMPORT_FAILURE",
                        "Import is failing.",
                        "We are investigating the import issue.",
                        "IMPORT_ENGINEERING",
                        1.5
                );

        when(persistenceService.markProcessing(ticketId))
                .thenReturn(ticket);

        when(provider.analyze(ticket))
                .thenReturn(invalidAnalysis);

        worker.analyze(ticketId);

        verify(persistenceService)
                .markProcessing(ticketId);

        verify(provider)
                .analyze(ticket);

        verify(persistenceService)
                .markFailed(
                        eq(ticketId),
                        eq("LLM output failed validation")
                );

        verify(persistenceService, never())
                .completeAnalysis(any(), any());
    }

    private static Ticket ticket() {

        var ticket = new Ticket();

        ticket.setCustomerId("CUST-1001");

        ticket.setSubject("Import failed");

        ticket.setDescription(
                "Document import failed at 80 percent"
        );

        ticket.setPriority(Priority.HIGH);

        ticket.setProduct("Support Portal");

        ticket.setAnalysisStatus(
                AnalysisStatus.PENDING
        );

        return ticket;
    }
}