package com.example.supportassistant.llm;

import com.example.supportassistant.domain.Priority;
import com.example.supportassistant.domain.Ticket;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MockLlmProviderTest {

    @Test
    void shouldClassifyImportTicket() {
        var ticket = new Ticket();
        ticket.setSubject("Unable to import Word document");
        ticket.setDescription("The import remains at 80% and fails.");
        ticket.setPriority(Priority.HIGH);

        var result = new MockLlmProvider().analyze(ticket);

        assertThat(result.category()).isEqualTo("IMPORT_FAILURE");
        assertThat(result.confidence()).isBetween(0.0, 1.0);
        assertThat(result.summary()).isNotBlank();
    }
}
