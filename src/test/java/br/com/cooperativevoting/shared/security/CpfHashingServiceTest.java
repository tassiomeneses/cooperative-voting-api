package br.com.cooperativevoting.shared.security;

import br.com.cooperativevoting.voting.domain.model.vo.Cpf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CpfHashingServiceTest {

    @Test
    void shouldHashCpfDeterministicallyWithoutReturningRawCpf() {
        CpfHashingService service = new CpfHashingService(new IdentityHashProperties("test-pepper", false));
        Cpf cpf = Cpf.from("529.982.247-25");

        String firstHash = service.hash(cpf);
        String secondHash = service.hash(cpf);

        assertEquals(firstHash, secondHash);
        assertEquals(64, firstHash.length());
        assertFalse(firstHash.contains(cpf.value()));
    }

    @Test
    void shouldRejectDefaultPepperWhenCustomPepperIsRequired() {
        assertThrows(
            IllegalStateException.class,
            () -> new CpfHashingService(new IdentityHashProperties("", true))
        );
    }
}
