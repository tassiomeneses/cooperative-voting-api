package br.com.cooperativevoting.voting.domain.factory;

import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.VoteId;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.time.Clock;
import java.time.Instant;

public final class VoteFactory {

    private VoteFactory() {
    }

    public static Vote cast(
        Agenda agenda,
        AssociateId associateId,
        VoteChoice choice,
        Clock clock
    ) {
        DomainValidator.notNull(clock, "clock");
        return cast(agenda, associateId, choice, Instant.now(clock));
    }

    public static Vote cast(
        Agenda agenda,
        AssociateId associateId,
        VoteChoice choice,
        Instant castAt
    ) {
        DomainValidator.notNull(agenda, "agenda");
        DomainValidator.notNull(castAt, "castAt");

        VotingSession session = agenda.currentSession()
            .orElseThrow(() -> VotingSessionClosedException.noSessionFor(agenda.getId()));

        session.ensureOpenAt(castAt);

        return Vote.cast(
            VoteId.newId(),
            agenda.getId(),
            associateId,
            choice,
            castAt
        );
    }
}
