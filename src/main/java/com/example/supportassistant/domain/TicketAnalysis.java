package com.example.supportassistant.domain;

import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

@Entity
@Table(name = "ticket_analyses", uniqueConstraints = {
        @UniqueConstraint(name = "uk_analysis_ticket", columnNames = "ticket_id")
})
public class TicketAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false, columnDefinition = "text")
    private String summary;

    @Column(name = "suggested_response", nullable = false, columnDefinition = "text")
    private String suggestedResponse;

    @Column(name = "recommended_team", nullable = false, length = 120)
    private String recommendedTeam;

    @Column(nullable = false)
    private double confidence;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    void prePersist() {
        createdAt = OffsetDateTime.now(ZoneOffset.UTC);
    }

    public UUID getId() { return id; }
    public Ticket getTicket() { return ticket; }
    public String getCategory() { return category; }
    public String getSummary() { return summary; }
    public String getSuggestedResponse() { return suggestedResponse; }
    public String getRecommendedTeam() { return recommendedTeam; }
    public double getConfidence() { return confidence; }
    public OffsetDateTime getCreatedAt() { return createdAt; }

    public void setTicket(Ticket ticket) { this.ticket = ticket; }
    public void setCategory(String category) { this.category = category; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setSuggestedResponse(String suggestedResponse) { this.suggestedResponse = suggestedResponse; }
    public void setRecommendedTeam(String recommendedTeam) { this.recommendedTeam = recommendedTeam; }
    public void setConfidence(double confidence) { this.confidence = confidence; }
}
