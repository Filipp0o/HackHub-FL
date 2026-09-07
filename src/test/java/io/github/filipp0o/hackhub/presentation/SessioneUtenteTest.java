package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SessioneUtenteTest {

    private final SessioneUtente sessione = new SessioneUtente();
    private final Utente utente = Utente.ricostruisci(
            1L, "utente@example.com", "hash-memorizzato"
    );

    @Test
    void sessioneInizialeNonContieneUtentiAutenticati() {
        assertThrows(
                SessioneUtente.UtenteNonAutenticatoException.class,
                sessione::recupera
        );
    }

    @Test
    void recuperaLUtenteRegistrato() {
        sessione.registra(utente);

        assertSame(utente, sessione.recupera());
    }

    @Test
    void unaNuovaRegistrazioneSostituisceLUtentePrecedente() {
        Utente altro = Utente.ricostruisci(
                2L, "altro@example.com", "hash-altro"
        );
        sessione.registra(utente);
        sessione.registra(altro);

        assertSame(altro, sessione.recupera());
    }

    @Test
    void rifiutaUtenteNulloConservandoLaSessioneCorrente() {
        sessione.registra(utente);

        assertThrows(NullPointerException.class, () -> sessione.registra(null));
        assertSame(utente, sessione.recupera());
    }

    @Test
    void svuotareLaSessioneRimuoveLUtenteAutenticato() {
        sessione.registra(utente);
        sessione.svuota();

        assertThrows(
                SessioneUtente.UtenteNonAutenticatoException.class,
                sessione::recupera
        );
    }

    @Test
    void svuotamentoRipetutoConsenteUnaSuccessivaRegistrazione() {
        sessione.svuota();
        sessione.svuota();
        sessione.registra(utente);

        assertSame(utente, sessione.recupera());
    }
}
