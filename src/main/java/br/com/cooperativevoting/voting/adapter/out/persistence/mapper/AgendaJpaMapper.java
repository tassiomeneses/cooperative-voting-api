package br.com.cooperativevoting.voting.adapter.out.persistence.mapper;

import br.com.cooperativevoting.voting.adapter.out.persistence.entity.AgendaJpaEntity;
import br.com.cooperativevoting.voting.adapter.out.persistence.entity.VotingSessionJpaEntity;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaDescription;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaTitle;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionId;
import org.springframework.stereotype.Component;

@Component
public class AgendaJpaMapper {

    public Agenda toDomain(AgendaJpaEntity entity) {
        VotingSession votingSession = null;
        if (entity.getVotingSession() != null) {
            votingSession = VotingSession.restore(
                VotingSessionId.from(entity.getVotingSession().getId()),
                AgendaId.from(entity.getId()),
                entity.getVotingSession().getOpenedAt(),
                entity.getVotingSession().getClosedAt()
            );
        }

        return Agenda.restore(
            AgendaId.from(entity.getId()),
            AgendaTitle.from(entity.getTitle()),
            AgendaDescription.from(entity.getDescription()),
            entity.getCreatedAt(),
            votingSession
        );
    }

    public AgendaJpaEntity toNewEntity(Agenda agenda) {
        AgendaJpaEntity entity = new AgendaJpaEntity(
            agenda.getId().value(),
            agenda.getTitle().value(),
            agenda.getDescription().value(),
            agenda.getCreatedAt()
        );
        agenda.currentSession()
            .map(this::toVotingSessionEntity)
            .ifPresent(entity::attachVotingSession);
        return entity;
    }

    public void updateEntity(AgendaJpaEntity entity, Agenda agenda) {
        entity.update(agenda.getTitle().value(), agenda.getDescription().value());

        agenda.currentSession().ifPresent(session -> {
            if (entity.getVotingSession() != null
                && !entity.getVotingSession().getId().equals(session.getId().value())) {
                throw new VotingSessionAlreadyOpenedException(agenda.getId());
            }

            if (entity.getVotingSession() == null) {
                entity.attachVotingSession(toVotingSessionEntity(session));
            }
        });
    }

    private VotingSessionJpaEntity toVotingSessionEntity(VotingSession session) {
        return new VotingSessionJpaEntity(
            session.getId().value(),
            session.getOpenedAt(),
            session.getClosedAt()
        );
    }
}
