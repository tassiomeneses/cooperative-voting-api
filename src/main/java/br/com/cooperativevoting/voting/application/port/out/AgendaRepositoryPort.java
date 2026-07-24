package br.com.cooperativevoting.voting.application.port.out;

import br.com.cooperativevoting.voting.domain.model.Agenda;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;

import java.util.Optional;

public interface AgendaRepositoryPort {

    Agenda save(Agenda agenda);

    Optional<Agenda> findById(AgendaId agendaId);

    Optional<VoteTally> findVoteTallyById(AgendaId agendaId);
}
