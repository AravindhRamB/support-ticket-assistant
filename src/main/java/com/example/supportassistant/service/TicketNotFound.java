package com.example.supportassistant.service;

import java.util.UUID;

public class TicketNotFound extends RuntimeException {
    public TicketNotFound(UUID ticketId) {
        super("Ticket not found: " + ticketId);
    }
}
