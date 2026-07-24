package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaDescription;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaTitle;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;
import java.util.Optional;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Agenda {

    @EqualsAndHashCode.Include
    private final AgendaId id;
    private final AgendaTitle title;
    private final AgendaDescription description;
    private final Instant createdAt;
    private final VotingSession votingSession;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private Agenda(
        AgendaId id,
        AgendaTitle title,
        AgendaDescription description,
        Instant createdAt,
        VotingSession votingSession
    ) {
        this.id = DomainValidator.notNull(id, "agendaId");
        this.title = DomainValidator.notNull(title, "agendaTitle");
        this.description = description == null ? AgendaDescription.empty() : description;
        this.createdAt = DomainValidator.notNull(createdAt, "createdAt");
        this.votingSession = votingSession;

        if (votingSession != null) {
            DomainValidator.require(votingSession.belongsTo(id), "votingSession must belong to agenda");
        }
    }

    public static Agenda create(
        AgendaId id,
        AgendaTitle title,
        AgendaDescription description,
        Instant createdAt
    ) {
        return Agenda.builder()
            .id(id)
            .title(title)
            .description(description)
            .createdAt(createdAt)
            .build();
    }

    public static Agenda restore(
        AgendaId id,
        AgendaTitle title,
        AgendaDescription description,
        Instant createdAt,
        VotingSession votingSession
    ) {
        return Agenda.builder()
            .id(id)
            .title(title)
            .description(description)
            .createdAt(createdAt)
            .votingSession(votingSession)
            .build();
    }

    public Optional<VotingSession> currentSession() {
        return Optional.ofNullable(votingSession);
    }

    public Agenda withVotingSession(VotingSession session) {
        DomainValidator.notNull(session, "votingSession");

        if (votingSession != null) {
            throw new VotingSessionAlreadyOpenedException(id);
        }

        DomainValidator.require(session.belongsTo(id), "votingSession must belong to agenda");

        return toBuilder()
            .votingSession(session)
            .build();
    }

    public boolean hasOpenSessionAt(Instant referenceTime) {
        DomainValidator.notNull(referenceTime, "referenceTime");
        return currentSession()
            .map(session -> session.isOpenAt(referenceTime))
            .orElse(false);
    }

    public void ensureVotingSessionOpenAt(Instant referenceTime) {
        if (!hasOpenSessionAt(referenceTime)) {
            throw VotingSessionClosedException.noSessionFor(id);
        }
    }
}
