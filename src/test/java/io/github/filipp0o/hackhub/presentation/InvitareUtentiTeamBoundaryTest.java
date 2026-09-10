package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.InvitareUtentiTeamControl;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.infrastructure.InMemoryInvitoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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

    @ParameterizedTest
    @EnumSource(FaseErrore.class)
    void erroreDelRepositoryRestituisce500EConsenteNuovoInvio(
            FaseErrore fase
    ) throws Exception {
        var utenti = mock(UtenteRepository.class);
        var teams = mock(TeamRepository.class);
        var inviti = spy(new InMemoryInvitoRepository());
        var creatore = new Utente(1L);
        var destinatario = Utente.ricostruisci(2L, "a@example.com", "hash");
        var team = Team.crea("ByteBuilders", creatore, creatore);
        sessione.registra(creatore);

        when(teams.recuperaTeamCreatoDa(creatore)).thenReturn(team);
        when(utenti.recuperaUtentiInvitabili(creatore)).thenReturn(List.of(destinatario));
        var errore = new IllegalArgumentException("Dettagli interni del repository");
        switch (fase) {
            case RECUPERO_TEAM ->
                    when(teams.recuperaTeamCreatoDa(creatore)).thenThrow(errore);
            case RICONTROLLO_UTENTI ->
                    when(utenti.recuperaUtentiInvitabili(creatore)).thenThrow(errore);
            case SALVATAGGIO_INVITO ->
                    doThrow(errore).when(inviti).salva(any(Invito.class));
        }

        var controlReale = new InvitareUtentiTeamControl(utenti, teams, inviti);
        var mvc = MockMvcBuilders.standaloneSetup(
                new InvitareUtentiTeamBoundary(controlReale, sessione)
        ).build();

        mvc.perform(post("/api/inviti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"utenteInvitatoId\":2}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.messaggio").value("Invito non registrato"))
                .andExpect(jsonPath("$.utentiInvitabili").isEmpty());

        assertAll(
                () -> assertTrue(inviti.recuperaInvitiRicevuti(destinatario).isEmpty()),
                () -> assertEquals(1, team.numeroMembri()),
                () -> assertSame(creatore, sessione.recupera())
        );

        doReturn(team).when(teams).recuperaTeamCreatoDa(creatore);
        doReturn(List.of(destinatario)).when(utenti).recuperaUtentiInvitabili(creatore);
        doCallRealMethod().when(inviti).salva(any(Invito.class));

        mvc.perform(get("/api/inviti/utenti-invitabili"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.utentiInvitabili[0].id").value(2));
        mvc.perform(post("/api/inviti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"utenteInvitatoId\":2}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.messaggio").value("Invito registrato"));

        var ricevuti = inviti.recuperaInvitiRicevuti(destinatario);
        assertEquals(1, ricevuti.size());
        assertAll(
                () -> assertNotNull(ricevuti.getFirst().getId()),
                () -> assertSame(team, ricevuti.getFirst().ottieniTeam()),
                () -> assertSame(destinatario, ricevuti.getFirst().getDestinatario()),
                () -> assertFalse(ricevuti.getFirst().isAccettato()),
                () -> assertEquals(1, team.numeroMembri()),
                () -> assertSame(creatore, sessione.recupera())
        );
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "", "{", "null", "{\"utenteInvitatoId\":\"non-numerico\"}"
    })
    void corpoAssenteOIlleggibileRestituisceEsitoInvitoSenzaInvocareControl(
            String corpo
    ) throws Exception {
        var utente = new Utente(1L);
        sessione.registra(utente);
        var mvc = MockMvcBuilders.standaloneSetup(boundary).build();

        mvc.perform(post("/api/inviti")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(corpo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.messaggio")
                        .value("È necessario selezionare un utente"))
                .andExpect(jsonPath("$.utentiInvitabili").isEmpty());

        verifyNoInteractions(control);
        assertSame(utente, sessione.recupera());
    }

    private enum FaseErrore {
        RECUPERO_TEAM,
        RICONTROLLO_UTENTI,
        SALVATAGGIO_INVITO
    }
}