package br.com.cooperativevoting.voting.application.usecase.createagenda.service;

import br.com.cooperativevoting.voting.application.port.in.CreateAgendaUseCase;
import br.com.cooperativevoting.voting.application.port.out.AgendaRepositoryPort;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaInput;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaOutput;
import br.com.cooperativevoting.voting.application.usecase.createagenda.mapper.CreateAgendaMapper;
import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.validation.DomainValidator;

import java.time.Clock;
import java.time.Instant;

public class CreateAgendaService implements CreateAgendaUseCase {

    private final AgendaRepositoryPort agendaRepositoryPort;
    private final CreateAgendaMapper mapper;
    private final Clock clock;

    public CreateAgendaService(
        AgendaRepositoryPort agendaRepositoryPort,
        CreateAgendaMapper mapper,
        Clock clock
    ) {
        this.agendaRepositoryPort = DomainValidator.notNull(agendaRepositoryPort, "agendaRepositoryPort");
        this.mapper = DomainValidator.notNull(mapper, "createAgendaMapper");
        this.clock = DomainValidator.notNull(clock, "clock");
    }

    @Override
    public CreateAgendaOutput execute(CreateAgendaInput input) {
        DomainValidator.notNull(input, "input");

        Instant createdAt = Instant.now(clock);
        Agenda agenda = mapper.toDomain(input, createdAt);
        Agenda savedAgenda = agendaRepositoryPort.save(agenda);

        return mapper.toOutput(savedAgenda);
    }
}
