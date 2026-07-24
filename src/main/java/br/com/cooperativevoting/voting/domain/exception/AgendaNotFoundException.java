package br.com.cooperativevoting.voting.domain.exception;

import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;

public class AgendaNotFoundException extends DomainException {

    public AgendaNotFoundException(AgendaId agendaId) {
        super("AGENDA_NOT_FOUND", "Agenda not found: " + agendaId);
    }
}
