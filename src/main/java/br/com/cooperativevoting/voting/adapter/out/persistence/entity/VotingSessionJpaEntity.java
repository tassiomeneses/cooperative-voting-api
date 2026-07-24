package br.com.cooperativevoting.voting.adapter.out.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "voting_sessions",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_voting_sessions_agenda_id",
        columnNames = "agenda_id"
    )
)
public class VotingSessionJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
        name = "agenda_id",
        nullable = false,
        foreignKey = @ForeignKey(name = "fk_voting_sessions_agenda")
    )
    private AgendaJpaEntity agenda;

    @Column(name = "opened_at", nullable = false)
    private Instant openedAt;

    @Column(name = "closed_at", nullable = false)
    private Instant closedAt;

    protected VotingSessionJpaEntity() {
    }

    public VotingSessionJpaEntity(UUID id, Instant openedAt, Instant closedAt) {
        this.id = id;
        this.openedAt = openedAt;
        this.closedAt = closedAt;
    }

    public UUID getId() {
        return id;
    }

    public AgendaJpaEntity getAgenda() {
        return agenda;
    }

    public Instant getOpenedAt() {
        return openedAt;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    void attachAgenda(AgendaJpaEntity agenda) {
        this.agenda = agenda;
    }
}
