package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.AccettareInvitoTeamControl;
import io.github.filipp0o.hackhub.domain.*;
import io.github.filipp0o.hackhub.infrastructure.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccettareInvitoTeamBoundaryTest {

    private final InMemoryInvitoRepository inviti =
            new InMemoryInvitoRepository();
    private final InMemoryTeamRepository teams =
            new InMemoryTeamRepository();
    private final InMemoryPartecipazioneRepository partecipazioni =
            new InMemoryPartecipazioneRepository();

    private final AccettareInvitoTeamControl control =
            new AccettareInvitoTeamControl(inviti, teams, partecipazioni);
    private final SessioneUtente sessione = new SessioneUtente();
    private final AccettareInvitoTeamBoundary boundary =
            new AccettareInvitoTeamBoundary(control, sessione);

    private final Utente creatore = new Utente(1L);
    private final Utente destinatario = new Utente(2L);
    private final Team team = Team.crea(
            "ByteBuilders", creatore, creatore
    );

    private Invito prepara() {
        teams.salva(team);
        Invito invito = Invito.crea(team, destinatario);
        inviti.salva(invito);
        sessione.registra(destinatario);
        return invito;
    }

    private void invariato(Invito invito) {
        assertFalse(invito.isAccettato());
        assertEquals(List.of(creatore), team.getMembri());
        assertEquals(
                List.of(invito),
                inviti.recuperaInvitiRicevuti(destinatario)
        );
    }

    @Test
    void richiedeSessionePerElencoEAccettazione() {
        Invito invito = prepara();
        sessione.svuota();

        assertEquals(
                401,
                boundary.selezionaAccettazioneInvitoTeam()
                        .getStatusCode().value()
        );
        assertEquals(
                401,
                boundary.selezionaInvito(invito).getStatusCode().value()
        );
        invariato(invito);
    }

    @Test
    void elencoMostraSoloInvitiPendentiDelDestinatario() {
        sessione.registra(destinatario);
        assertEquals(
                "Nessun invito disponibile",
                boundary.selezionaAccettazioneInvitoTeam()
                        .getBody().messaggio()
        );

        Invito invito = prepara();
        inviti.salva(Invito.crea(team, new Utente(3L)));

        Invito accettato = Invito.crea(team, destinatario);
        accettato.registraAccettazione();
        inviti.salva(accettato);

        var risposta = boundary.selezionaAccettazioneInvitoTeam();

        assertEquals(200, risposta.getStatusCode().value());
        assertEquals(
                List.of(new AccettareInvitoTeamBoundary.InvitoRicevuto(
                        invito.getId(), team.getId(), "ByteBuilders"
                )),
                risposta.getBody().invitiRicevuti()
        );
        invariato(invito);
    }

    @Test
    void accettaEAggiungeMembroUnaSolaVolta() {
        Invito invito = prepara();

        var risposta = boundary.selezionaInvito(invito);

        assertEquals(200, risposta.getStatusCode().value());
        assertEquals(
                "Accettazione completata",
                risposta.getBody().messaggio()
        );
        assertTrue(invito.isAccettato());
        assertSame(team, teams.recuperaTeam(destinatario));
        assertTrue(inviti.recuperaInvitiRicevuti(destinatario).isEmpty());

        assertEquals(
                404,
                boundary.selezionaInvito(invito).getStatusCode().value()
        );
        assertEquals(2, team.numeroMembri());
    }

    @Test
    void selezioneAssenteONonValidaNonModificaDati() {
        Invito invito = prepara();

        assertEquals(
                400, boundary.accetta(null).getStatusCode().value()
        );

        for (Long id : new Long[]{null, 0L, -1L}) {
            assertEquals(
                    400,
                    boundary.accetta(
                            new AccettareInvitoTeamBoundary
                                    .RichiestaAccettazione(id)
                    ).getStatusCode().value()
            );
        }
        invariato(invito);
    }

    @Test
    void invitoAltruiEIdSconosciutoNonSonoDisponibili() {
        Invito invito = prepara();
        sessione.registra(new Utente(3L));

        assertEquals(
                404,
                boundary.selezionaInvito(invito).getStatusCode().value()
        );
        assertEquals(
                404,
                boundary.accetta(
                        new AccettareInvitoTeamBoundary
                                .RichiestaAccettazione(999L)
                ).getStatusCode().value()
        );
        invariato(invito);
    }

    @Test
    void appartenenzaAdAltroTeamRestituisceConflict() {
        Invito invito = prepara();
        teams.salva(Team.crea("Altro", destinatario, destinatario));

        var risposta = boundary.selezionaInvito(invito);

        assertEquals(409, risposta.getStatusCode().value());
        assertEquals(
                "L'utente appartiene già a un team",
                risposta.getBody().messaggio()
        );
        invariato(invito);
    }

    @Test
    void capienzaSuperataRestituisceConflict() {
        Invito invito = prepara();
        Hackathon hackathon = mock(Hackathon.class);

        when(hackathon.getStato())
                .thenReturn(TipoStatoHackathon.IN_CORSO);
        when(hackathon.rispettaDimensioneMassima(2))
                .thenReturn(false);

        partecipazioni.salva(Partecipazione.crea(hackathon, team));

        var risposta = boundary.selezionaInvito(invito);

        assertEquals(409, risposta.getStatusCode().value());
        assertEquals(
                "L'ingresso supera la dimensione massima consentita per il team",
                risposta.getBody().messaggio()
        );
        invariato(invito);
    }

    @Test
    void erroriOperativiNonEspongonoDettagliInterni() {
        Invito invito = prepara();
        var guasto = mock(AccettareInvitoTeamControl.class);
        var altra = new AccettareInvitoTeamBoundary(guasto, sessione);

        when(guasto.richiediInvitiRicevuti(destinatario))
                .thenThrow(new IllegalStateException("Dettagli SQL"));

        var elenco = altra.selezionaAccettazioneInvitoTeam();

        assertEquals(500, elenco.getStatusCode().value());
        assertEquals(
                "Recupero degli inviti non completato",
                elenco.getBody().messaggio()
        );

        reset(guasto);
        when(guasto.richiediInvitiRicevuti(destinatario))
                .thenReturn(List.of(invito));
        doThrow(new IllegalStateException("Dettagli SQL"))
                .when(guasto)
                .richiediAccettazioneInvito(destinatario, invito);

        var risposta = altra.selezionaInvito(invito);

        assertEquals(500, risposta.getStatusCode().value());
        assertEquals(
                "Accettazione non completata",
                risposta.getBody().messaggio()
        );
        invariato(invito);
    }
}