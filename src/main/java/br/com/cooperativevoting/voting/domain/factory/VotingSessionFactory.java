package br.com.cooperativevoting.voting.domain.factory;

import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionId;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.time.Clock;
import java.time.Instant;

public final class VotingSessionFactory {

    private VotingSessionFactory() {
    }

    public static VotingSession openFor(Agenda agenda, VotingSessionDuration duration, Clock clock) {
        DomainValidator.notNull(clock, "clock");
        return openFor(agenda, duration, Instant.now(clock));
    }

    public static VotingSession openFor(Agenda agenda, VotingSessionDuration duration, Instant openedAt) {
        DomainValidator.notNull(agenda, "agenda");
        return VotingSession.open(
            VotingSessionId.newId(),
            agenda.getId(),
            duration,
            openedAt
        );
    }

    public static VotingSession openDefaultFor(Agenda agenda, Clock clock) {
        return openFor(agenda, VotingSessionDuration.DEFAULT, clock);
    }
}
