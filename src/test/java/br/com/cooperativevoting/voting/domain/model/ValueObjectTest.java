package br.com.cooperativevoting.voting.domain.model;

import br.com.cooperativevoting.voting.domain.exception.InvalidDomainObjectException;
import br.com.cooperativevoting.voting.domain.model.enums.VoteChoice;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaId;
import br.com.cooperativevoting.voting.domain.model.vo.AgendaTitle;
import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import br.com.cooperativevoting.voting.domain.model.vo.VotingSessionDuration;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ValueObjectTest {

    @Test
    void shouldNormalizeAndValidateCpf() {
        Cpf cpf = Cpf.from("529.982.247-25");

        assertEquals("52998224725", cpf.value());
        assertEquals("529.***.***-25", cpf.masked());
    }

    @Test
    void shouldRejectInvalidCpf() {
        assertThrows(InvalidDomainObjectException.class, () -> Cpf.from("111.111.111-11"));
    }

    @Test
    void shouldRejectBlankAgendaTitle() {
        assertThrows(InvalidDomainObjectException.class, () -> AgendaTitle.from(" "));
    }

    @Test
    void shouldRejectInvalidUuid() {
        assertThrows(InvalidDomainObjectException.class, () -> AgendaId.from("not-a-uuid"));
    }

    @Test
    void shouldRejectNonPositiveVotingSessionDuration() {
        assertThrows(
            InvalidDomainObjectException.class,
            () -> VotingSessionDuration.from(Duration.ZERO)
        );
    }

    @Test
    void shouldRejectVotingSessionDurationAboveMaximum() {
        assertThrows(
            InvalidDomainObjectException.class,
            () -> VotingSessionDuration.from(Duration.ofMinutes(1441))
        );
    }

    @Test
    void shouldParsePortugueseVoteChoice() {
        assertEquals(VoteChoice.YES, VoteChoice.from("Sim"));
        assertEquals(VoteChoice.NO, VoteChoice.from("N\u00e3o"));
    }
}
