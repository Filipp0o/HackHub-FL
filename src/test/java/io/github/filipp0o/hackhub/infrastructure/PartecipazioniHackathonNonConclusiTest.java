package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PartecipazioniHackathonNonConclusiTest {

    private final PartecipazioneRepository repository = new InMemoryPartecipazioneRepository();
    private final Utente responsabile = new Utente(1L);
    private final Team team = Team.crea("ByteBuilders", responsabile, responsabile);

    private Partecipazione salva(Team destinatario, TipoStatoHackathon stato) {
        Hackathon hackathon = mock(Hackathon.class);
        when(hackathon.getStato()).thenReturn(stato);
        Partecipazione partecipazione = Partecipazione.crea(hackathon, destinatario);
        repository.salva(partecipazione);
        return partecipazione;
    }

    @Test
    void includeTuttiGliStatiTranneConcluso() {
        List<Partecipazione> attese = new ArrayList<>();
        for (TipoStatoHackathon stato : TipoStatoHackathon.values()) {
            Partecipazione partecipazione = salva(team, stato);
            if (stato != TipoStatoHackathon.CONCLUSO) {
                attese.add(partecipazione);
            }
        }

        assertEquals(attese, repository.recuperaPartecipazioniInHackathonNonConclusi(team));
    }

    @Test
    void selezionaTeamPerIdAncheConIstanzaDiversa() {
        team.assegnaId(10L);
        Team copia = Team.ricostruisci(10L, "ByteBuilders", List.of(responsabile), responsabile);
        Team altro = Team.crea("Altro", responsabile, responsabile);
        altro.assegnaId(11L);
        Partecipazione propria = salva(team, TipoStatoHackathon.IN_ISCRIZIONE);
        salva(altro, TipoStatoHackathon.IN_ISCRIZIONE);

        assertEquals(List.of(propria), repository.recuperaPartecipazioniInHackathonNonConclusi(copia));
    }

    @Test
    void teamSenzaIdSonoDistintiPerIdentitaDiOggetto() {
        Team altro = Team.crea("Altro", responsabile, responsabile);
        Partecipazione propria = salva(team, TipoStatoHackathon.IN_ISCRIZIONE);
        salva(altro, TipoStatoHackathon.IN_ISCRIZIONE);

        assertEquals(List.of(propria), repository.recuperaPartecipazioniInHackathonNonConclusi(team));
    }

    @Test
    void partecipazioneEsclusaRestaInclusaSeHackathonNonConcluso() {
        Partecipazione esclusa = salva(team, TipoStatoHackathon.IN_VALUTAZIONE);
        esclusa.escludi();

        assertEquals(List.of(esclusa), repository.recuperaPartecipazioniInHackathonNonConclusi(team));
    }

    @Test
    void nuovaLetturaRifletteConclusioneHackathon() {
        Partecipazione partecipazione = salva(team, TipoStatoHackathon.IN_VALUTAZIONE);
        assertEquals(List.of(partecipazione), repository.recuperaPartecipazioniInHackathonNonConclusi(team));

        when(partecipazione.ottieniHackathon().getStato()).thenReturn(TipoStatoHackathon.CONCLUSO);

        assertTrue(repository.recuperaPartecipazioniInHackathonNonConclusi(team).isEmpty());
        assertEquals(List.of(partecipazione), repository.ottieniPartecipazioni(partecipazione.ottieniHackathon()));
    }

    @Test
    void risultatoEUnoSnapshotNonModificabile() {
        Partecipazione prima = salva(team, TipoStatoHackathon.IN_ISCRIZIONE);
        var risultato = repository.recuperaPartecipazioniInHackathonNonConclusi(team);
        assertThrows(UnsupportedOperationException.class, risultato::clear);
        salva(team, TipoStatoHackathon.IN_VALUTAZIONE);

        assertEquals(List.of(prima), risultato);
        assertEquals(2, repository.recuperaPartecipazioniInHackathonNonConclusi(team).size());
    }

    @Test
    void nessunaPartecipazioneRestituisceListaVuotaETeamNulloVieneRifiutato() {
        assertTrue(repository.recuperaPartecipazioniInHackathonNonConclusi(team).isEmpty());
        assertThrows(NullPointerException.class,
                () -> repository.recuperaPartecipazioniInHackathonNonConclusi(null));
    }
}
