package br.com.cooperativevoting.voting.adapter.out.persistence;

import br.com.cooperativevoting.shared.security.CpfHashingService;
import br.com.cooperativevoting.voting.adapter.out.persistence.entity.VoteJpaEntity;
import br.com.cooperativevoting.voting.adapter.out.persistence.mapper.VoteJpaMapper;
import br.com.cooperativevoting.voting.adapter.out.persistence.repository.SpringDataVoteRepository;
import br.com.cooperativevoting.voting.application.port.out.VoteRepositoryPort;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.exception.DuplicateVoteException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class JpaVoteRepositoryAdapter implements VoteRepositoryPort {

    private final SpringDataVoteRepository repository;
    private final VoteJpaMapper mapper;
    private final CpfHashingService cpfHashingService;

    public JpaVoteRepositoryAdapter(
        SpringDataVoteRepository repository,
        VoteJpaMapper mapper,
        CpfHashingService cpfHashingService
    ) {
        this.repository = repository;
        this.mapper = mapper;
        this.cpfHashingService = cpfHashingService;
    }

    @Override
    @Transactional
    public Vote saveIfSessionOpen(Vote vote, Cpf cpf) {
        String voterKey = cpfHashingService.hash(cpf);
        try {
            int insertedRows = repository.insertIfSessionOpen(
                vote.getId().value(),
                vote.getAgendaId().value(),
                vote.getAssociateId().value(),
                voterKey,
                vote.getChoice().name(),
                vote.getCastAt()
            );
            if (insertedRows == 0) {
                if (!repository.existsAgendaById(vote.getAgendaId().value())) {
                    throw new AgendaNotFoundException(vote.getAgendaId());
                }
                throw new VotingSessionClosedException(vote.getAgendaId());
            }
            VoteJpaEntity entity = mapper.toEntity(vote, voterKey);
            return mapper.toDomain(entity);
        } catch (DataIntegrityViolationException exception) {
            if (ConstraintViolationDetector.hasConstraint(
                exception,
                "uk_votes_agenda_associate",
                "uk_votes_agenda_voter_key"
            )) {
                throw new DuplicateVoteException(vote.getAgendaId(), vote.getAssociateId());
            }
            throw exception;
        }
    }

}
