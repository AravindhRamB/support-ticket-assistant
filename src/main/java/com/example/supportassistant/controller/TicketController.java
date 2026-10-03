package com.example.supportassistant.controller;

import com.example.supportassistant.api.ApiModels.CreateTicketRequest;
import com.example.supportassistant.api.ApiModels.PageResponse;
import com.example.supportassistant.api.ApiModels.TicketResponse;
import com.example.supportassistant.api.ApiModels.TicketSummaryResponse;
import com.example.supportassistant.domain.AnalysisStatus;
import com.example.supportassistant.service.TicketService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/tickets")
@Tag(name = "Support Tickets")
public class TicketController {

    private final TicketService ticketService;

    public TicketController(TicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @Operation(summary = "Create a support ticket")
    public ResponseEntity<TicketResponse> create(@Valid @RequestBody CreateTicketRequest request) {
        TicketResponse response = ticketService.create(request);
        return ResponseEntity
                .created(URI.create("/api/v1/tickets/" + response.ticketId()))
                .body(response);
    }

    @GetMapping("/{ticketId}")
    @Operation(summary = "Retrieve a ticket and its current analysis")
    public TicketResponse get(@PathVariable UUID ticketId) {
        return ticketService.get(ticketId);
    }

    @GetMapping
    @Operation(summary = "List tickets with optional status/customer filtering")
    public PageResponse<TicketSummaryResponse> list(
            @RequestParam(required = false) String customerId,
            @RequestParam(required = false) AnalysisStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 100);

        var pageable = PageRequest.of(
                safePage,
                safeSize,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        return ticketService.list(customerId, status, pageable);
    }
}
