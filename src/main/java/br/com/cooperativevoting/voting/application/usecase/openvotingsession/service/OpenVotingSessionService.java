package br.com.cooperativevoting.voting.application.usecase.openvotingsession.service;

import br.com.cooperativevoting.voting.application.port.in.OpenVotingSessionUseCase;
import br.com.cooperativevoting.voting.application.port.out.AgendaRepositoryPort;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionInput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.mapper.OpenVotingSessionMapper;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.factory.VotingSessionFactory;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.time.Clock;
import java.time.Instant;

public class OpenVotingSessionService implements OpenVotingSessionUseCase {

    private final AgendaRepositoryPort agendaRepositoryPort;
    private final OpenVotingSessionMapper mapper;
    private final Clock clock;

    public OpenVotingSessionService(
        AgendaRepositoryPort agendaRepositoryPort,
        OpenVotingSessionMapper mapper,
        Clock clock
    ) {
        this.agendaRepositoryPort = DomainValidator.notNull(agendaRepositoryPort, "agendaRepositoryPort");
        this.mapper = DomainValidator.notNull(mapper, "openVotingSessionMapper");
        this.clock = DomainValidator.notNull(clock, "clock");
    }

    @Override
    public OpenVotingSessionOutput execute(OpenVotingSessionInput input) {
        DomainValidator.notNull(input, "input");

        AgendaId agendaId = mapper.toAgendaId(input);
        Agenda agenda = agendaRepositoryPort.findById(agendaId)
            .orElseThrow(() -> new AgendaNotFoundException(agendaId));

        Instant openedAt = Instant.now(clock);
        VotingSessionDuration duration = mapper.toDuration(input);
        VotingSession session = VotingSessionFactory.openFor(agenda, duration, openedAt);

        Agenda savedAgenda = agendaRepositoryPort.save(agenda.withVotingSession(session));
        VotingSession savedSession = savedAgenda.currentSession().orElse(session);

        return mapper.toOutput(savedSession, openedAt);
    }
}
