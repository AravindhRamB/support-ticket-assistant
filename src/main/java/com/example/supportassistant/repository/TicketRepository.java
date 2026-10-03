package com.example.supportassistant.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.example.supportassistant.domain.AnalysisStatus;
import com.example.supportassistant.domain.Priority;
import com.example.supportassistant.domain.Ticket;

public interface TicketRepository extends JpaRepository<Ticket, UUID> {

    Page<Ticket> findByAnalysisStatus(
            AnalysisStatus status,
            Pageable pageable
    );

    Page<Ticket> findByPriority(
            Priority priority,
            Pageable pageable
    );

    Page<Ticket> findByCustomerIdIgnoreCase(
            String customerId,
            Pageable pageable
    );

    Page<Ticket> findByCustomerIdIgnoreCaseAndAnalysisStatus(
            String customerId,
            AnalysisStatus status,
            Pageable pageable
    );
}