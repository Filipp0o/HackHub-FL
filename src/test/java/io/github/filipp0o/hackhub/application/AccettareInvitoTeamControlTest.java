package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.*;
import io.github.filipp0o.hackhub.infrastructure.InMemoryInvitoRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryPartecipazioneRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryTeamRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccettareInvitoTeamControlTest {

    private final InvitoRepository inviti = new InMemoryInvitoRepository();
    private final TeamRepository teams = new InMemoryTeamRepository();
    private final PartecipazioneRepository partecipazioni = new InMemoryPartecipazioneRepository();
    private final AccettareInvitoTeamControl control =
            new AccettareInvitoTeamControl(inviti, teams, partecipazioni);
    private final Utente creatore = new Utente(1L);
    private final Utente destinatario = new Utente(2L);
    private final Team team = Team.crea("ByteBuilders", creatore, creatore);

    private Invito preparaInvito() {
        teams.salva(team);
        Invito invito = Invito.crea(team, destinatario);
        inviti.salva(invito);
        return invito;
    }

    private void iscrivi(TipoStatoHackathon stato, int limite) {
        Hackathon hackathon = mock(Hackathon.class);
        when(hackathon.getStato()).thenReturn(stato);
        when(hackathon.rispettaDimensioneMassima(anyInt()))
                .thenAnswer(chiamata -> (Integer) chiamata.getArgument(0) <= limite);
        partecipazioni.salva(Partecipazione.crea(hackathon, team));
    }

    private void verificaInvariato(Invito invito) {
        assertFalse(invito.isAccettato());
        assertEquals(List.of(creatore), team.getMembri());
        assertEquals(List.of(invito), inviti.recuperaInvitiRicevuti(destinatario));
    }

    @Test
    void recuperaInvitiRicevutiSenzaModifiche() {
        assertTrue(control.richiediInvitiRicevuti(destinatario).isEmpty());
        Invito invito = preparaInvito();
        assertEquals(List.of(invito), control.richiediInvitiRicevuti(destinatario));
        verificaInvariato(invito);
    }

    @Test
    void accettaSenzaPartecipazioniEAggiungeIlMembro() {
        Invito invito = preparaInvito();
        control.richiediAccettazioneInvito(destinatario, invito);

        assertTrue(invito.isAccettato());
        assertEquals(List.of(creatore, destinatario), team.getMembri());
        assertSame(team, teams.recuperaTeam(destinatario));
        assertTrue(control.richiediInvitiRicevuti(destinatario).isEmpty());
    }

    @Test
    void rifiutaInvitoDiUnAltroDestinatario() {
        Invito invito = preparaInvito();
        assertThrows(AccettareInvitoTeamControl.InvitoNonDisponibileException.class,
                () -> control.richiediAccettazioneInvito(new Utente(3L), invito));
        verificaInvariato(invito);
    }

    @Test
    void rifiutaInvitoNulloNonSalvatoOSconosciuto() {
        Invito invito = preparaInvito();
        for (Invito selezionato : new Invito[]{null, Invito.crea(team, destinatario),
                Invito.ricostruisci(99L, team, destinatario, false)}) {
            assertThrows(AccettareInvitoTeamControl.InvitoNonDisponibileException.class,
                    () -> control.richiediAccettazioneInvito(destinatario, selezionato));
        }
        verificaInvariato(invito);
    }

    @Test
    void usaInvitoDelRepositoryAncheSeLaSelezioneContieneUnTeamDiverso() {
        Invito invito = preparaInvito();
        Team altro = Team.crea("Altro", creatore, creatore);
        Invito selezione = Invito.ricostruisci(invito.getId(), altro, creatore, false);

        control.richiediAccettazioneInvito(new Utente(destinatario.getId()), selezione);

        assertTrue(invito.isAccettato());
        assertFalse(selezione.isAccettato());
        assertEquals(2, team.numeroMembri());
        assertEquals(1, altro.numeroMembri());
    }

    @Test
    void rifiutaUtenteGiaInUnTeamSenzaAccettareInvito() {
        Invito invito = preparaInvito();
        teams.salva(Team.crea("Altro", destinatario, destinatario));

        assertThrows(AccettareInvitoTeamControl.UtenteGiaInTeamException.class,
                () -> control.richiediAccettazioneInvito(destinatario, invito));
        verificaInvariato(invito);
    }

    @Test
    void accettaAlLimiteEsattoDiTuttiGliHackathonNonConclusi() {
        Invito invito = preparaInvito();
        iscrivi(TipoStatoHackathon.IN_ISCRIZIONE, 3);
        iscrivi(TipoStatoHackathon.IN_CORSO, 2);
        iscrivi(TipoStatoHackathon.IN_VALUTAZIONE, 2);
        iscrivi(TipoStatoHackathon.CONCLUSO, 1);

        control.richiediAccettazioneInvito(destinatario, invito);

        assertTrue(invito.isAccettato());
        assertEquals(2, team.numeroMembri());
    }

    @Test
    void bastaUnHackathonConLimiteSuperatoPerRifiutareIngresso() {
        Invito invito = preparaInvito();
        iscrivi(TipoStatoHackathon.IN_ISCRIZIONE, 5);
        iscrivi(TipoStatoHackathon.IN_VALUTAZIONE, 1);

        assertThrows(AccettareInvitoTeamControl.DimensioneMassimaSuperataException.class,
                () -> control.richiediAccettazioneInvito(destinatario, invito));
        verificaInvariato(invito);
    }

    @Test
    void secondaAccettazioneNonDuplicaMembro() {
        Invito invito = preparaInvito();
        control.richiediAccettazioneInvito(destinatario, invito);

        assertThrows(AccettareInvitoTeamControl.InvitoNonDisponibileException.class,
                () -> control.richiediAccettazioneInvito(destinatario, invito));
        assertEquals(2, team.numeroMembri());
    }

    @Test
    void secondoInvitoRivalutaDimensioneDopoIlPrimoIngresso() {
        Invito primo = preparaInvito();
        Utente altro = new Utente(3L);
        Invito secondo = Invito.crea(team, altro);
        inviti.salva(secondo);
        iscrivi(TipoStatoHackathon.IN_CORSO, 2);
        control.richiediAccettazioneInvito(destinatario, primo);

        assertThrows(AccettareInvitoTeamControl.DimensioneMassimaSuperataException.class,
                () -> control.richiediAccettazioneInvito(altro, secondo));
        assertFalse(secondo.isAccettato());
        assertEquals(2, team.numeroMembri());
        assertFalse(teams.verificaAppartenenzaTeam(altro));
    }

    @Test
    void erroreNelRecuperoPartecipazioniNonModificaInvitoOTeam() {
        Invito invito = preparaInvito();
        PartecipazioneRepository guasto = mock(PartecipazioneRepository.class);
        var errore = new IllegalStateException("Recupero non disponibile");
        when(guasto.recuperaPartecipazioniInHackathonNonConclusi(team)).thenThrow(errore);
        var altroControl = new AccettareInvitoTeamControl(inviti, teams, guasto);

        assertSame(errore, assertThrows(IllegalStateException.class,
                () -> altroControl.richiediAccettazioneInvito(destinatario, invito)));
        verificaInvariato(invito);
    }

    @Test
    void rifiutaDipendenzeEArgomentiObbligatoriNulli() {
        assertThrows(NullPointerException.class,
                () -> new AccettareInvitoTeamControl(null, teams, partecipazioni));
        assertThrows(NullPointerException.class,
                () -> new AccettareInvitoTeamControl(inviti, null, partecipazioni));
        assertThrows(NullPointerException.class,
                () -> new AccettareInvitoTeamControl(inviti, teams, null));
        assertThrows(NullPointerException.class, () -> control.richiediInvitiRicevuti(null));
        assertThrows(NullPointerException.class, () -> control.richiediAccettazioneInvito(null, null));
        assertThrows(NullPointerException.class, () -> control.verificaAmmissibilitaNuovoMembro(null));
    }
}
