package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.model.enums.VotingResultOutcome;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@Getter
@ToString
public class VotingResult {

    private final AgendaId agendaId;
    private final long yesVotes;
    private final long noVotes;

    @Builder(toBuilder = true, access = AccessLevel.PRIVATE)
    private VotingResult(AgendaId agendaId, long yesVotes, long noVotes) {
        this.agendaId = DomainValidator.notNull(agendaId, "agendaId");
        this.yesVotes = DomainValidator.notNegative(yesVotes, "yesVotes");
        this.noVotes = DomainValidator.notNegative(noVotes, "noVotes");
    }

    public static VotingResult of(AgendaId agendaId, long yesVotes, long noVotes) {
        return VotingResult.builder()
            .agendaId(agendaId)
            .yesVotes(yesVotes)
            .noVotes(noVotes)
            .build();
    }

    public long totalVotes() {
        return yesVotes + noVotes;
    }

    public VotingResultOutcome outcome() {
        if (yesVotes > noVotes) {
            return VotingResultOutcome.APPROVED;
        }

        if (noVotes > yesVotes) {
            return VotingResultOutcome.REJECTED;
        }

        return VotingResultOutcome.TIED;
    }
}
