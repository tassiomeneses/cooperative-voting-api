package br.com.cooperativevoting.voting.adapter.out.persistence.mapper;

import br.com.cooperativevoting.voting.adapter.out.persistence.entity.VoteJpaEntity;
import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.VoteId;
import org.springframework.stereotype.Component;

@Component
public class VoteJpaMapper {

    public VoteJpaEntity toEntity(Vote vote, String voterKey) {
        return new VoteJpaEntity(
            vote.getId().value(),
            vote.getAgendaId().value(),
            vote.getAssociateId().value(),
            voterKey,
            vote.getChoice(),
            vote.getCastAt()
        );
    }

    public Vote toDomain(VoteJpaEntity entity) {
        return Vote.cast(
            VoteId.from(entity.getId()),
            AgendaId.from(entity.getAgendaId()),
            AssociateId.from(entity.getAssociateId()),
            entity.getChoice(),
            entity.getCastAt()
        );
    }
}
