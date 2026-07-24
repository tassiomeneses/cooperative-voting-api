package br.com.cooperativevoting.voting.application.usecase.registervote.service;

import br.com.cooperativevoting.voting.application.port.out.AssociateEligibilityPort;
import br.com.cooperativevoting.voting.application.port.out.VoteRepositoryPort;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteInput;
import br.com.cooperativevoting.voting.application.usecase.registervote.dto.RegisterVoteOutput;
import br.com.cooperativevoting.voting.application.usecase.registervote.mapper.RegisterVoteMapper;
import br.com.cooperativevoting.voting.domain.exception.AgendaNotFoundException;
import br.com.cooperativevoting.voting.domain.exception.AssociateUnableToVoteException;
import br.com.cooperativevoting.voting.domain.exception.DuplicateVoteException;
import br.com.cooperativevoting.voting.domain.exception.VotingSessionClosedException;
import br.com.cooperativevoting.voting.domain.model.Vote;
import br.com.cooperativevoting.voting.domain.model.enums.AssociateVotingStatus;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AssociateId;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterVoteServiceTest {

    private static final Instant NOW = Instant.parse("2026-07-24T12:00:00Z");
    private static final String VALID_CPF = "529.982.247-25";

    @Mock
    private VoteRepositoryPort voteRepositoryPort;

    @Mock
    private AssociateEligibilityPort associateEligibilityPort;

    private RegisterVoteService service;

    @BeforeEach
    void setUp() {
        service = new RegisterVoteService(
            voteRepositoryPort,
            associateEligibilityPort,
            new RegisterVoteMapper(),
            Clock.fixed(NOW, ZoneOffset.UTC)
        );
    }

    @Test
    void shouldRegisterVoteWhenAssociateIsAbleAndSessionIsOpen() {
        AgendaId agendaId = AgendaId.newId();
        RegisterVoteInput input = inputFor(agendaId, "associate-1", "Sim");

        when(associateEligibilityPort.check(Cpf.from(VALID_CPF))).thenReturn(AssociateVotingStatus.ABLE_TO_VOTE);
        when(voteRepositoryPort.saveIfSessionOpen(any(Vote.class), any(Cpf.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        RegisterVoteOutput output = service.execute(input);

        assertEquals(agendaId.toString(), output.agendaId());
        assertEquals("associate-1", output.associateId());
        assertEquals(VoteChoice.YES, output.choice());
        assertEquals(NOW, output.castAt());
        verify(voteRepositoryPort).saveIfSessionOpen(any(Vote.class), any(Cpf.class));
    }

    @Test
    void shouldRejectWhenAgendaDoesNotExist() {
        AgendaId agendaId = AgendaId.newId();
        when(associateEligibilityPort.check(Cpf.from(VALID_CPF))).thenReturn(AssociateVotingStatus.ABLE_TO_VOTE);
        when(voteRepositoryPort.saveIfSessionOpen(any(Vote.class), any(Cpf.class)))
            .thenThrow(new AgendaNotFoundException(agendaId));

        assertThrows(AgendaNotFoundException.class, () -> service.execute(inputFor(agendaId, "associate-1", "Sim")));
        verify(voteRepositoryPort).saveIfSessionOpen(any(Vote.class), any(Cpf.class));
    }

    @Test
    void shouldRejectWhenAssociateIsUnableToVote() {
        AgendaId agendaId = AgendaId.newId();
        when(associateEligibilityPort.check(Cpf.from(VALID_CPF))).thenReturn(AssociateVotingStatus.UNABLE_TO_VOTE);

        assertThrows(
            AssociateUnableToVoteException.class,
            () -> service.execute(inputFor(agendaId, "associate-1", "Sim"))
        );
        verify(voteRepositoryPort, never()).saveIfSessionOpen(any(Vote.class), any(Cpf.class));
    }

    @Test
    void shouldRejectDuplicateVote() {
        AgendaId agendaId = AgendaId.newId();
        AssociateId associateId = AssociateId.from("associate-1");
        when(associateEligibilityPort.check(Cpf.from(VALID_CPF))).thenReturn(AssociateVotingStatus.ABLE_TO_VOTE);
        when(voteRepositoryPort.saveIfSessionOpen(any(Vote.class), any(Cpf.class)))
            .thenThrow(new DuplicateVoteException(agendaId, associateId));

        assertThrows(
            DuplicateVoteException.class,
            () -> service.execute(inputFor(agendaId, associateId.value(), "Sim"))
        );
        verify(voteRepositoryPort).saveIfSessionOpen(any(Vote.class), any(Cpf.class));
    }

    @Test
    void shouldRejectWhenVotingSessionIsClosed() {
        AgendaId agendaId = AgendaId.newId();
        when(associateEligibilityPort.check(Cpf.from(VALID_CPF))).thenReturn(AssociateVotingStatus.ABLE_TO_VOTE);
        when(voteRepositoryPort.saveIfSessionOpen(any(Vote.class), any(Cpf.class)))
            .thenThrow(new VotingSessionClosedException(agendaId));

        assertThrows(
            VotingSessionClosedException.class,
            () -> service.execute(inputFor(agendaId, "associate-1", "Nao"))
        );
        verify(voteRepositoryPort).saveIfSessionOpen(any(Vote.class), any(Cpf.class));
    }

    private static RegisterVoteInput inputFor(AgendaId agendaId, String associateId, String choice) {
        return new RegisterVoteInput(agendaId.toString(), associateId, VALID_CPF, choice);
    }

}
