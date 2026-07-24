package br.com.cooperativevoting.voting.application.port.in;

import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaInput;
import br.com.cooperativevoting.voting.application.usecase.createagenda.dto.CreateAgendaOutput;

public interface CreateAgendaUseCase {

    CreateAgendaOutput execute(CreateAgendaInput input);
}
