package com.example.supportassistant.service;

import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.supportassistant.api.ApiModels.AnalysisResponse;
import com.example.supportassistant.api.ApiModels.CreateTicketRequest;
import com.example.supportassistant.api.ApiModels.PageResponse;
import com.example.supportassistant.api.ApiModels.TicketResponse;
import com.example.supportassistant.api.ApiModels.TicketSummaryResponse;
import com.example.supportassistant.domain.AnalysisStatus;
import com.example.supportassistant.domain.Ticket;
import com.example.supportassistant.domain.TicketAnalysis;
import com.example.supportassistant.repository.TicketAnalysisRepository;
import com.example.supportassistant.repository.TicketRepository;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketAnalysisRepository analysisRepository;
    private final ApplicationEventPublisher eventPublisher;

    public TicketService(
            TicketRepository ticketRepository,
            TicketAnalysisRepository analysisRepository,
            ApplicationEventPublisher eventPublisher
    ) {
        this.ticketRepository = ticketRepository;
        this.analysisRepository = analysisRepository;
        this.eventPublisher = eventPublisher;
    }

    // ============================================================
    // CREATE TICKET
    // ============================================================

    @Transactional
    public TicketResponse create(CreateTicketRequest request) {

        var ticket = new Ticket();

        ticket.setCustomerId(request.customerId().trim());
        ticket.setSubject(request.subject().trim());
        ticket.setDescription(request.description().trim());
        ticket.setPriority(request.priority());
        ticket.setProduct(normalizeOptional(request.product()));

        ticket.setAnalysisStatus(AnalysisStatus.PENDING);
        ticket.setAnalysisError(null);

        ticket = ticketRepository.save(ticket);

        /*
         * Publish the event after the ticket has been saved.
         */
        eventPublisher.publishEvent(
                new TicketCreated(ticket.getId())
        );

        return toResponse(ticket, null);
    }

    // ============================================================
    // GET SINGLE TICKET
    // ============================================================

    @Transactional(readOnly = true)
    public TicketResponse get(UUID ticketId) {

        Ticket ticket = findTicket(ticketId);

        TicketAnalysis analysis =
                analysisRepository.findByTicketId(ticketId)
                        .orElse(null);

        return toResponse(ticket, analysis);
    }

    // ============================================================
    // LIST / FILTER TICKETS
    // ============================================================

    @Transactional(readOnly = true)
    public PageResponse<TicketSummaryResponse> list(
            String customerId,
            AnalysisStatus status,
            Pageable pageable
    ) {

        Page<Ticket> page;

        boolean hasCustomerId =
                customerId != null && !customerId.isBlank();

        boolean hasStatus =
                status != null;

        /*
         * CASE 1:
         */
        if (hasCustomerId && hasStatus) {

            page = ticketRepository
                    .findByCustomerIdIgnoreCaseAndAnalysisStatus(
                            customerId.trim(),
                            status,
                            pageable
                    );
        }

        /*
         * CASE 2:
         *
         * Only customerId is provided.
         *
         */
        else if (hasCustomerId) {

            page = ticketRepository
                    .findByCustomerIdIgnoreCase(
                            customerId.trim(),
                            pageable
                    );
        }

        /*
         * CASE 3:
         *
         * Only status is provided.
         */
        else if (hasStatus) {

            page = ticketRepository
                    .findByAnalysisStatus(
                            status,
                            pageable
                    );
        }

        /*
         * CASE 4:
         *
         */
        else {

            page = ticketRepository.findAll(pageable);
        }

        var content = page.getContent()
                .stream()
                .map(this::toSummary)
                .toList();

        return new PageResponse<>(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    // ============================================================
    // FIND TICKET
    // ============================================================

    private Ticket findTicket(UUID ticketId) {

        return ticketRepository
                .findById(ticketId)
                .orElseThrow(
                        () -> new TicketNotFound(ticketId)
                );
    }

    // ============================================================
    // BUILD FULL RESPONSE
    // ============================================================

    private TicketResponse toResponse(
            Ticket ticket,
            TicketAnalysis analysis
    ) {

        AnalysisResponse analysisResponse =
                analysis == null
                        ? null
                        : new AnalysisResponse(
                                analysis.getCategory(),
                                analysis.getSummary(),
                                analysis.getSuggestedResponse(),
                                analysis.getRecommendedTeam(),
                                analysis.getConfidence()
                        );

        return new TicketResponse(
                ticket.getId(),
                ticket.getCustomerId(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getProduct(),
                ticket.getAnalysisStatus(),
                analysisResponse,
                ticket.getAnalysisError(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    // ============================================================
    // BUILD LIST RESPONSE
    // ============================================================

    private TicketSummaryResponse toSummary(Ticket ticket) {

        return new TicketSummaryResponse(
                ticket.getId(),
                ticket.getCustomerId(),
                ticket.getSubject(),
                ticket.getPriority(),
                ticket.getProduct(),
                ticket.getAnalysisStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    // ============================================================
    // NORMALIZE OPTIONAL PRODUCT
    // ============================================================

    private String normalizeOptional(String value) {

        return value == null || value.isBlank()
                ? null
                : value.trim();
    }

    // ============================================================
    // EVENT
    // ============================================================

    public record TicketCreated(UUID ticketId) {
    }
}