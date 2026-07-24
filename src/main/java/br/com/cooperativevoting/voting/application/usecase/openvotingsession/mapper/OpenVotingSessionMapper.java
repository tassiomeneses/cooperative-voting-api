package br.com.cooperativevoting.voting.application.usecase.openvotingsession.mapper;

import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionInput;
import br.com.cooperativevoting.voting.application.usecase.openvotingsession.dto.OpenVotingSessionOutput;
import br.com.cooperativevoting.voting.domain.model.VotingSession;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;

import java.time.Instant;

public class OpenVotingSessionMapper {

    public AgendaId toAgendaId(OpenVotingSessionInput input) {
        return AgendaId.from(input.agendaId());
    }

    public VotingSessionDuration toDuration(OpenVotingSessionInput input) {
        if (input.durationMinutes() == null) {
            return null;
        }
        return VotingSessionDuration.fromMinutes(input.durationMinutes());
    }

    public OpenVotingSessionOutput toOutput(VotingSession session, Instant referenceTime) {
        return new OpenVotingSessionOutput(
            session.getAgendaId().toString(),
            session.getId().toString(),
            session.getOpenedAt(),
            session.getClosedAt(),
            session.statusAt(referenceTime)
        );
    }
}
