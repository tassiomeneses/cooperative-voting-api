package br.com.cooperativevoting.voting.adapter.out.persistence.entity;

import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
    name = "votes",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_votes_agenda_associate",
        columnNames = {"agenda_id", "associate_id"}
    ),
    indexes = @Index(name = "idx_votes_agenda_choice", columnList = "agenda_id, choice")
)
public class VoteJpaEntity {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "agenda_id", nullable = false, updatable = false)
    private UUID agendaId;

    @Column(name = "associate_id", nullable = false, updatable = false, length = 64)
    private String associateId;

    @Column(name = "voter_key", nullable = false, updatable = false, length = 64)
    private String voterKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "choice", nullable = false, updatable = false, length = 3)
    private VoteChoice choice;

    @Column(name = "cast_at", nullable = false, updatable = false)
    private Instant castAt;

    protected VoteJpaEntity() {
    }

    public VoteJpaEntity(UUID id, UUID agendaId, String associateId, String voterKey, VoteChoice choice, Instant castAt) {
        this.id = id;
        this.agendaId = agendaId;
        this.associateId = associateId;
        this.voterKey = voterKey;
        this.choice = choice;
        this.castAt = castAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getAgendaId() {
        return agendaId;
    }

    public String getAssociateId() {
        return associateId;
    }

    public String getVoterKey() {
        return voterKey;
    }

    public VoteChoice getChoice() {
        return choice;
    }

    public Instant getCastAt() {
        return castAt;
    }
}
