package br.com.cooperativevoting.voting.application.usecase.findvotingresult.service;

import br.com.cooperativevoting.voting.application.port.out.AgendaRepositoryPort;
import br.com.cooperativevoting.voting.application.port.out.VoteTally;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultInput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.dto.FindVotingResultOutput;
import br.com.cooperativevoting.voting.application.usecase.findvotingresult.mapper.FindVotingResultMapper;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.model.enums.VotingResultOutcome;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindVotingResultServiceTest {

    @Mock
    private AgendaRepositoryPort agendaRepositoryPort;

    private FindVotingResultService service;

    @BeforeEach
    void setUp() {
        service = new FindVotingResultService(
            agendaRepositoryPort,
            new FindVotingResultMapper()
        );
    }

    @Test
    void shouldFindVotingResult() {
        AgendaId agendaId = AgendaId.newId();
        when(agendaRepositoryPort.findVoteTallyById(agendaId)).thenReturn(Optional.of(new VoteTally(7, 3)));

        FindVotingResultOutput output = service.execute(new FindVotingResultInput(agendaId.toString()));

        assertEquals(agendaId.toString(), output.agendaId());
        assertEquals(7, output.yesVotes());
        assertEquals(3, output.noVotes());
        assertEquals(10, output.totalVotes());
        assertEquals(VotingResultOutcome.APPROVED, output.outcome());
    }

    @Test
    void shouldFindTiedVotingResult() {
        AgendaId agendaId = AgendaId.newId();
        when(agendaRepositoryPort.findVoteTallyById(agendaId)).thenReturn(Optional.of(new VoteTally(4, 4)));

        FindVotingResultOutput output = service.execute(new FindVotingResultInput(agendaId.toString()));

        assertEquals(VotingResultOutcome.TIED, output.outcome());
    }

    @Test
    void shouldRejectWhenAgendaDoesNotExist() {
        AgendaId agendaId = AgendaId.newId();
        when(agendaRepositoryPort.findVoteTallyById(agendaId)).thenReturn(Optional.empty());

        assertThrows(
            AgendaNotFoundException.class,
            () -> service.execute(new FindVotingResultInput(agendaId.toString()))
        );
    }
}
