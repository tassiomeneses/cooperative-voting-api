package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.VoteId;
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
public class Vote {

    @EqualsAndHashCode.Include
    private final VoteId id;
    private final AgendaId agendaId;
    private final AssociateId associateId;
    private final VoteChoice choice;
    private final Instant castAt;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private Vote(
        VoteId id,
        AgendaId agendaId,
        AssociateId associateId,
        VoteChoice choice,
        Instant castAt
    ) {
        this.id = DomainValidator.notNull(id, "voteId");
        this.agendaId = DomainValidator.notNull(agendaId, "agendaId");
        this.associateId = DomainValidator.notNull(associateId, "associateId");
        this.choice = DomainValidator.notNull(choice, "voteChoice");
        this.castAt = DomainValidator.notNull(castAt, "castAt");
    }

    public static Vote cast(
        VoteId id,
        AgendaId agendaId,
        AssociateId associateId,
        VoteChoice choice,
        Instant castAt
    ) {
        return Vote.builder()
            .id(id)
            .agendaId(agendaId)
            .associateId(associateId)
            .choice(choice)
            .castAt(castAt)
            .build();
    }

    public boolean belongsTo(AgendaId agendaId) {
        return this.agendaId.equals(agendaId);
    }

    public boolean isYes() {
        return choice.isYes();
    }

    public boolean isNo() {
        return choice.isNo();
    }
}
