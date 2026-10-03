package com.example.supportassistant.service;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.example.supportassistant.domain.AnalysisStatus;
import com.example.supportassistant.domain.Ticket;
import com.example.supportassistant.domain.TicketAnalysis;
import com.example.supportassistant.llm.LlmAnalysis;
import com.example.supportassistant.repository.TicketAnalysisRepository;
import com.example.supportassistant.repository.TicketRepository;

@Service
public class TicketAnalysisPersistenceService {

    private final TicketRepository ticketRepository;
    private final TicketAnalysisRepository analysisRepository;

    public TicketAnalysisPersistenceService(
            TicketRepository ticketRepository,
            TicketAnalysisRepository analysisRepository) {

        this.ticketRepository = ticketRepository;
        this.analysisRepository = analysisRepository;
    }

    /**
     * Transaction 1.
     *
     * Loads the ticket and changes:
     *
     * PENDING -> PROCESSING
     *
     * The transaction commits before the LLM call starts.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Ticket markProcessing(UUID ticketId) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElse(null);

        if (ticket == null) {
            return null;
        }

        ticket.setAnalysisStatus(
                AnalysisStatus.PROCESSING
        );

        ticket.setAnalysisError(null);

        return ticketRepository.save(ticket);
    }

    /**
     * Transaction 2.
     *
     * Saves the validated LLM analysis and changes:
     *
     * PROCESSING -> COMPLETED
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeAnalysis(
            UUID ticketId,
            LlmAnalysis result) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Ticket not found: " + ticketId
                        )
                );

        TicketAnalysis analysis =
                analysisRepository.findByTicketId(ticketId)
                        .orElseGet(TicketAnalysis::new);

        analysis.setTicket(ticket);

        analysis.setCategory(
                result.category().trim()
        );

        analysis.setSummary(
                result.summary().trim()
        );

        analysis.setSuggestedResponse(
                result.suggestedResponse().trim()
        );

        analysis.setRecommendedTeam(
                result.recommendedTeam().trim()
        );

        analysis.setConfidence(
                result.confidence()
        );

        analysisRepository.save(analysis);

        ticket.setAnalysisStatus(
                AnalysisStatus.COMPLETED
        );

        ticket.setAnalysisError(null);

        ticketRepository.save(ticket);
    }

    /**
     * Transaction used when the LLM fails,
     * times out, or returns invalid data.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(
            UUID ticketId,
            String errorMessage) {

        Ticket ticket = ticketRepository.findById(ticketId)
                .orElse(null);

        if (ticket == null) {

            return;
        }

        ticket.setAnalysisStatus(
                AnalysisStatus.FAILED
        );

        ticket.setAnalysisError(
                errorMessage
        );

        ticketRepository.save(ticket);
    }
}