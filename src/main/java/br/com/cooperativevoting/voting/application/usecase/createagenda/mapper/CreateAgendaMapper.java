package br.com.cooperativevoting.voting.application.usecase.createagenda.mapper;

import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaInput;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaOutput;
import br.com.cooperativevoting.voting.domain.factory.AgendaFactory;
import br.com.cooperativevoting.voting.domain.model.Agenda;

import java.time.Instant;

public class CreateAgendaMapper {

    public Agenda toDomain(CreateAgendaInput input, Instant createdAt) {
        return AgendaFactory.create(input.title(), input.description(), createdAt);
    }

    public CreateAgendaOutput toOutput(Agenda agenda) {
        return new CreateAgendaOutput(
            agenda.getId().toString(),
            agenda.getTitle().value(),
            agenda.getDescription().value(),
            agenda.getCreatedAt()
        );
    }
}
