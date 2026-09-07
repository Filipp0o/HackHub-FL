package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.EffettuareAccessoControl;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EffettuareAccessoBoundaryTest {

    @Test
    void erroreOperativoNonEsponeDettagliENonModificaSessione() {
        var control = mock(EffettuareAccessoControl.class);
        var sessione = new SessioneUtente();
        var utente = new Utente(1L);
        sessione.registra(utente);
        var richiesta = new MockHttpServletRequest();
        String id = richiesta.getSession().getId();
        when(control.richiediAccesso("a@example.com", "password"))
                .thenThrow(new IllegalStateException("Dettagli interni database"));

        var boundary = new EffettuareAccessoBoundary(control, sessione, richiesta);
        var risposta = boundary.inserisciCredenziali("a@example.com", "password");

        assertEquals(500, risposta.getStatusCode().value());
        assertEquals("Accesso non completato", risposta.getBody().messaggio());
        assertSame(utente, sessione.recupera());
        assertEquals(id, richiesta.getSession(false).getId());
    }

    @Test
    void richiestaNullaNonInvocaControlENonCreaSessione() {
        var control = mock(EffettuareAccessoControl.class);
        var richiesta = new MockHttpServletRequest();
        var boundary = new EffettuareAccessoBoundary(
                control, new SessioneUtente(), richiesta
        );

        assertEquals(401, boundary.accedi(null).getStatusCode().value());
        verifyNoInteractions(control);
        assertNull(richiesta.getSession(false));
    }
}
