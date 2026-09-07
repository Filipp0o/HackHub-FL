package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.InvitareUtentiTeamControl;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitareUtentiTeamBoundaryTest {

    private final InvitareUtentiTeamControl control = mock(InvitareUtentiTeamControl.class);
    private final SessioneUtente sessione = new SessioneUtente();
    private final InvitareUtentiTeamBoundary boundary = new InvitareUtentiTeamBoundary(control, sessione);

    @Test
    void elencoVuotoComunicaAssenzaDiUtenti() {
        Utente utente = new Utente(1L);
        sessione.registra(utente);
        when(control.richiediUtentiInvitabili(utente)).thenReturn(List.of());
        var risposta = boundary.selezionaInvitoUtenteTeam();
        assertEquals(200, risposta.getStatusCode().value());
        assertEquals("Nessun utente disponibile", risposta.getBody().messaggio());
        assertTrue(risposta.getBody().utentiInvitabili().isEmpty());
    }

    @Test
    void erroriOperativiRestituiscono500SenzaDettagliInterni() {
        Utente utente = new Utente(1L);
        sessione.registra(utente);
        when(control.richiediUtentiInvitabili(utente))
                .thenThrow(new IllegalStateException("Dettagli database"));
        doThrow(new IllegalStateException("Dettagli database"))
                .when(control).richiediInvito(eq(utente), any(Utente.class));

        var elenco = boundary.selezionaInvitoUtenteTeam();
        assertEquals(500, elenco.getStatusCode().value());
        assertEquals("Recupero degli utenti non completato", elenco.getBody().messaggio());
        var invio = boundary.selezionaUtente(new Utente(2L));
        assertEquals(500, invio.getStatusCode().value());
        assertEquals("Invito non registrato", invio.getBody().messaggio());
    }

    @Test
    void selezioneNullaRichiedeDestinatario() {
        sessione.registra(new Utente(1L));
        assertEquals(400, boundary.selezionaUtente(null).getStatusCode().value());
        verifyNoInteractions(control);
    }
}
