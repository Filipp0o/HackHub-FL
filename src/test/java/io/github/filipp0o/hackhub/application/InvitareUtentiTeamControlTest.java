package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.application.InvitareUtentiTeamControl.UtenteNonInvitabileException;
import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class InvitareUtentiTeamControlTest {

    private final UtenteRepository utenti = mock(UtenteRepository.class);
    private final TeamRepository teams = mock(TeamRepository.class);
    private final InvitoRepository inviti = mock(InvitoRepository.class);
    private final InvitareUtentiTeamControl control =
            new InvitareUtentiTeamControl(utenti, teams, inviti);
    private final Utente creatore = new Utente(1L);
    private final Utente destinatario = Utente.ricostruisci(2L, "a@example.com", "hash");
    private final Team team = Team.crea("ByteBuilders", creatore, creatore);

    @Test
    void richiedeElencoSenzaSalvareInviti() {
        when(utenti.recuperaUtentiInvitabili(creatore)).thenReturn(List.of(destinatario));

        assertEquals(List.of(destinatario), control.richiediUtentiInvitabili(creatore));
        verifyNoInteractions(teams, inviti);
    }

    @Test
    void gestisceElencoVuoto() {
        when(utenti.recuperaUtentiInvitabili(creatore)).thenReturn(List.of());

        assertTrue(control.richiediUtentiInvitabili(creatore).isEmpty());
        verifyNoInteractions(inviti);
    }

    @Test
    void salvaInvitoUsandoTeamEAccountRecuperati() {
        when(teams.recuperaTeamCreatoDa(creatore)).thenReturn(team);
        when(utenti.recuperaUtentiInvitabili(creatore)).thenReturn(List.of(destinatario));

        control.richiediInvito(creatore, new Utente(destinatario.getId()));

        var cattura = ArgumentCaptor.forClass(Invito.class);
        verify(inviti).salva(cattura.capture());
        Invito invito = cattura.getValue();
        assertSame(team, invito.ottieniTeam());
        assertSame(destinatario, invito.getDestinatario());
        assertFalse(invito.isAccettato());
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void destinatarioNonSelezionatoOSenzaIdNonAvviaRecuperi() {
        assertThrows(UtenteNonInvitabileException.class,
                () -> control.richiediInvito(creatore, null));
        assertThrows(UtenteNonInvitabileException.class,
                () -> control.richiediInvito(creatore, Utente.crea("x@example.com", "hash")));
        verifyNoInteractions(utenti, teams, inviti);
    }

    @Test
    void rifiutaSeStessoODestinatarioNonPiuInvitabile() {
        when(teams.recuperaTeamCreatoDa(creatore)).thenReturn(team);
        when(utenti.recuperaUtentiInvitabili(creatore)).thenReturn(List.of(destinatario));

        assertThrows(UtenteNonInvitabileException.class,
                () -> control.richiediInvito(creatore, new Utente(creatore.getId())));
        assertThrows(UtenteNonInvitabileException.class,
                () -> control.richiediInvito(creatore, new Utente(99L)));
        verifyNoInteractions(inviti);
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void richiedenteSenzaTeamCreatoNonPuoSalvareInvito() {
        when(teams.recuperaTeamCreatoDa(creatore))
                .thenThrow(new TeamRepository.TeamNonCreatoException());

        assertThrows(TeamRepository.TeamNonCreatoException.class,
                () -> control.richiediInvito(creatore, destinatario));
        verifyNoInteractions(utenti, inviti);
    }

    @Test
    void ricontrollaIlDestinatarioDopoLaVisualizzazioneDellElenco() {
        when(teams.recuperaTeamCreatoDa(creatore)).thenReturn(team);
        when(utenti.recuperaUtentiInvitabili(creatore))
                .thenReturn(List.of(destinatario), List.of());

        assertEquals(List.of(destinatario),
                control.richiediUtentiInvitabili(creatore));
        assertThrows(UtenteNonInvitabileException.class,
                () -> control.richiediInvito(creatore, destinatario));

        verifyNoInteractions(inviti);
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void erroreRecuperoUtentiInterrompeInvio() {
        when(teams.recuperaTeamCreatoDa(creatore)).thenReturn(team);
        var errore = new IllegalStateException("Recupero non disponibile");
        when(utenti.recuperaUtentiInvitabili(creatore)).thenThrow(errore);

        assertSame(errore, assertThrows(IllegalStateException.class,
                () -> control.richiediInvito(creatore, destinatario)));
        verifyNoInteractions(inviti);
    }

    @Test
    void propagaErroreSalvataggioSenzaModificareTeam() {
        when(teams.recuperaTeamCreatoDa(creatore)).thenReturn(team);
        when(utenti.recuperaUtentiInvitabili(creatore)).thenReturn(List.of(destinatario));
        var errore = new IllegalStateException("Salvataggio non disponibile");
        doThrow(errore).when(inviti).salva(any(Invito.class));

        assertSame(errore, assertThrows(IllegalStateException.class,
                () -> control.richiediInvito(creatore, destinatario)));
        assertEquals(1, team.numeroMembri());
    }

    @Test
    void rifiutaRichiedenteNullo() {
        assertThrows(NullPointerException.class, () -> control.richiediUtentiInvitabili(null));
        assertThrows(NullPointerException.class, () -> control.richiediInvito(null, destinatario));
        verifyNoInteractions(utenti, teams, inviti);
    }

    @Test
    void richiedeTuttiIRepository() {
        assertThrows(NullPointerException.class,
                () -> new InvitareUtentiTeamControl(null, teams, inviti));
        assertThrows(NullPointerException.class,
                () -> new InvitareUtentiTeamControl(utenti, null, inviti));
        assertThrows(NullPointerException.class,
                () -> new InvitareUtentiTeamControl(utenti, teams, null));
    }
}