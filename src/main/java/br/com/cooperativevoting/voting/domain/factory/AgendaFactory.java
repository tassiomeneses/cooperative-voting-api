package br.com.cooperativevoting.voting.domain.factory;

import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaDescription;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaTitle;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.time.Clock;
import java.time.Instant;

public final class AgendaFactory {

    private AgendaFactory() {
    }

    public static Agenda create(String title, String description, Clock clock) {
        DomainValidator.notNull(clock, "clock");
        return create(title, description, Instant.now(clock));
    }

    public static Agenda create(String title, String description, Instant createdAt) {
        return Agenda.create(
            AgendaId.newId(),
            AgendaTitle.from(title),
            AgendaDescription.from(description),
            createdAt
        );
    }

    public static Agenda restore(
        AgendaId id,
        AgendaTitle title,
        AgendaDescription description,
        Instant createdAt,
        VotingSession votingSession
    ) {
        return Agenda.restore(id, title, description, createdAt, votingSession);
    }
}
