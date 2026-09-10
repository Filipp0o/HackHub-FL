package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.EffettuareAccessoControl;
import io.github.filipp0o.hackhub.domain.Utente;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
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

    @ParameterizedTest
    @CsvSource({
            "RECUPERO_O_CREAZIONE, false",
            "RECUPERO_O_CREAZIONE, true",
            "ROTAZIONE_ID, false",
            "ROTAZIONE_ID, true"
    })
    void errorePreparazioneSessioneNonAutenticaEConsenteNuovoAccesso(
            PassaggioSessione passaggio,
            boolean giaAutenticato
    ) {
        var control = mock(EffettuareAccessoControl.class);
        var sessione = new SessioneUtente();
        var precedente = new Utente(1L);
        var nuovoUtente = new Utente(2L);
        var richiesta = new RichiestaConErroreSessione();

        String idPrecedente = null;
        if (giaAutenticato) {
            sessione.registra(precedente);
            richiesta.getSession().setAttribute("preferenza", "italiano");
            idPrecedente = richiesta.getSession().getId();
        }

        when(control.richiediAccesso("a@example.com", "password"))
                .thenReturn(nuovoUtente);
        var boundary = new EffettuareAccessoBoundary(control, sessione, richiesta);
        var dati = new EffettuareAccessoBoundary.RichiestaAccesso(
                "a@example.com", "password"
        );
        richiesta.passaggioFallito = passaggio;

        var fallimento = boundary.accedi(dati);

        assertEquals(500, fallimento.getStatusCode().value());
        assertNotNull(fallimento.getBody());
        assertEquals("Accesso non completato", fallimento.getBody().messaggio());
        if (giaAutenticato) {
            assertSame(precedente, sessione.recupera());
            assertEquals(idPrecedente, richiesta.getSession(false).getId());
            assertEquals("italiano",
                    richiesta.getSession(false).getAttribute("preferenza"));
        } else {
            assertThrows(SessioneUtente.UtenteNonAutenticatoException.class,
                    sessione::recupera);
            if (passaggio == PassaggioSessione.RECUPERO_O_CREAZIONE) {
                assertNull(richiesta.getSession(false));
            } else {
                assertNotNull(richiesta.getSession(false));
            }
        }

        var sessioneHttp = richiesta.getSession(false);
        String idPrimaDelNuovoAccesso = sessioneHttp == null
                ? null : sessioneHttp.getId();
        richiesta.passaggioFallito = null;

        var successo = boundary.accedi(dati);

        assertEquals(200, successo.getStatusCode().value());
        assertNotNull(successo.getBody());
        assertEquals("Accesso effettuato", successo.getBody().messaggio());
        assertSame(nuovoUtente, sessione.recupera());
        assertNotNull(richiesta.getSession(false));
        if (idPrimaDelNuovoAccesso != null) {
            assertNotEquals(idPrimaDelNuovoAccesso,
                    richiesta.getSession(false).getId());
        }
        if (giaAutenticato) {
            assertEquals("italiano",
                    richiesta.getSession(false).getAttribute("preferenza"));
        }
        verify(control, times(2)).richiediAccesso("a@example.com", "password");
    }

    private enum PassaggioSessione {
        RECUPERO_O_CREAZIONE,
        ROTAZIONE_ID
    }

    private static class RichiestaConErroreSessione
            extends MockHttpServletRequest {

        private PassaggioSessione passaggioFallito;

        @Override
        public HttpSession getSession() {
            if (passaggioFallito == PassaggioSessione.RECUPERO_O_CREAZIONE) {
                throw new IllegalStateException("Dettagli interni della sessione");
            }
            return super.getSession();
        }

        @Override
        public String changeSessionId() {
            if (passaggioFallito == PassaggioSessione.ROTAZIONE_ID) {
                throw new IllegalStateException("Dettagli interni della rotazione");
            }
            return super.changeSessionId();
        }
    }
}