package com.example.supportassistant.repository;

import com.example.supportassistant.domain.TicketAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TicketAnalysisRepository extends JpaRepository<TicketAnalysis, UUID> {
    Optional<TicketAnalysis> findByTicketId(UUID ticketId);
}
