package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.model.enums.VotingSessionStatus;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionId;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

import java.time.Instant;

@Getter
@ToString
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class VotingSession {

    @EqualsAndHashCode.Include
    private final VotingSessionId id;
    private final AgendaId agendaId;
    private final Instant openedAt;
    private final Instant closedAt;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private VotingSession(
        VotingSessionId id,
        AgendaId agendaId,
        Instant openedAt,
        Instant closedAt
    ) {
        this.id = DomainValidator.notNull(id, "votingSessionId");
        this.agendaId = DomainValidator.notNull(agendaId, "agendaId");
        this.openedAt = DomainValidator.notNull(openedAt, "openedAt");
        this.closedAt = DomainValidator.notNull(closedAt, "closedAt");

        DomainValidator.require(this.closedAt.isAfter(this.openedAt), "closedAt must be after openedAt");
    }

    public static VotingSession open(
        VotingSessionId id,
        AgendaId agendaId,
        VotingSessionDuration duration,
        Instant openedAt
    ) {
        VotingSessionDuration effectiveDuration = duration == null ? VotingSessionDuration.DEFAULT : duration;

        return VotingSession.builder()
            .id(id)
            .agendaId(agendaId)
            .openedAt(openedAt)
            .closedAt(openedAt.plus(effectiveDuration.value()))
            .build();
    }

    public static VotingSession restore(
        VotingSessionId id,
        AgendaId agendaId,
        Instant openedAt,
        Instant closedAt
    ) {
        return VotingSession.builder()
            .id(id)
            .agendaId(agendaId)
            .openedAt(openedAt)
            .closedAt(closedAt)
            .build();
    }

    public VotingSessionStatus statusAt(Instant referenceTime) {
        return VotingSessionStatus.at(openedAt, closedAt, referenceTime);
    }

    public boolean isOpenAt(Instant referenceTime) {
        return statusAt(referenceTime) == VotingSessionStatus.OPEN;
    }

    public void ensureOpenAt(Instant referenceTime) {
        if (!isOpenAt(referenceTime)) {
            throw new VotingSessionClosedException(agendaId);
        }
    }

    public boolean belongsTo(AgendaId agendaId) {
        return this.agendaId.equals(agendaId);
    }
}
