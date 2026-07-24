package br.com.cooperativevoting.voting.adapter.in.rest.mapper;

import br.com.cooperativevoting.voting.adapter.in.rest.dto.request.CreateAgendaRequest;
import br.com.cooperativevoting.voting.adapter.in.rest.dto.response.CreateAgendaResponse;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaInput;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaOutput;
import org.springframework.stereotype.Component;

@Component
public class AgendaRestMapper {

    public CreateAgendaInput toInput(CreateAgendaRequest request) {
        return new CreateAgendaInput(request.titulo(), request.descricao());
    }

    public CreateAgendaResponse toResponse(CreateAgendaOutput output) {
        return new CreateAgendaResponse(
            output.agendaId(),
            output.title(),
            output.description(),
            output.createdAt()
        );
    }
}
