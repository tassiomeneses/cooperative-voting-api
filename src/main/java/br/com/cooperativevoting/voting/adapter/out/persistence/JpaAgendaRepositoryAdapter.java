package br.com.cooperativevoting.voting.adapter.out.persistence;

import br.com.cooperativevoting.voting.adapter.out.persistence.entity.AgendaJpaEntity;
import br.com.cooperativevoting.voting.adapter.out.persistence.mapper.AgendaJpaMapper;
import br.com.cooperativevoting.voting.adapter.out.persistence.repository.SpringDataAgendaRepository;
import br.com.cooperativevoting.voting.application.port.out.AgendaRepositoryPort;
import br.com.cooperativevoting.voting.application.port.out.VoteTally;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionAlreadyOpenedException;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public class JpaAgendaRepositoryAdapter implements AgendaRepositoryPort {

    private final SpringDataAgendaRepository repository;
    private final AgendaJpaMapper mapper;

    public JpaAgendaRepositoryAdapter(SpringDataAgendaRepository repository, AgendaJpaMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    @Override
    @Transactional
    public Agenda save(Agenda agenda) {
        try {
            AgendaJpaEntity entity = repository.findByIdWithVotingSession(agenda.getId().value())
                .map(existingEntity -> {
                    mapper.updateEntity(existingEntity, agenda);
                    return existingEntity;
                })
                .orElseGet(() -> mapper.toNewEntity(agenda));

            AgendaJpaEntity savedEntity = repository.saveAndFlush(entity);
            return mapper.toDomain(savedEntity);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolationDetector.hasConstraint(exception, "uk_voting_sessions_agenda_id")) {
                throw new VotingSessionAlreadyOpenedException(agenda.getId());
            }
            throw exception;
        } catch (ObjectOptimisticLockingFailureException exception) {
            if (agenda.currentSession().isPresent()) {
                throw new VotingSessionAlreadyOpenedException(agenda.getId());
            }
            throw exception;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Agenda> findById(AgendaId agendaId) {
        return repository.findByIdWithVotingSession(agendaId.value())
            .map(mapper::toDomain);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VoteTally> findVoteTallyById(AgendaId agendaId) {
        return repository.findVoteTallyByAgendaId(agendaId.value())
            .map(tally -> new VoteTally(tally.getYesVotes(), tally.getNoVotes()));
    }
}
