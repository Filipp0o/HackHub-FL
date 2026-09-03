package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.DatiValutazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Valutazione;
import io.github.filipp0o.hackhub.infrastructure.InMemoryHackathonRepository;
import io.github.filipp0o.hackhub.infrastructure.InMemoryPartecipazioneRepository;
import io.github.filipp0o.hackhub.infrastructure.SegnalazioneRepositoryImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProclamareTeamVincitoreAmmissibilitaTest {

    @Test
    void restituisceSoloPartecipazioniConSottomissioneValutata() {
        Utente organizzatore = new Utente(1L);

        Hackathon hackathon = creaHackathonInValutazione(
                organizzatore
        );

        Partecipazione partecipazioneValutata =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );

        Partecipazione partecipazioneSenzaSottomissione =
                creaPartecipazione(
                        hackathon,
                        11L
                );

        InMemoryPartecipazioneRepository partecipazioneRepository =
                new InMemoryPartecipazioneRepository();

        partecipazioneRepository.salva(
                partecipazioneValutata
        );
        partecipazioneRepository.salva(
                partecipazioneSenzaSottomissione
        );

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new InMemoryHackathonRepository(
                                partecipazioneRepository
                        ),
                        new SegnalazioneRepositoryImpl()
                );

        List<Partecipazione> risultato =
                control.avviaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                );

        assertEquals(
                List.of(partecipazioneValutata),
                risultato
        );
    }

    private Hackathon creaHackathonInValutazione(
            Utente organizzatore
    ) {
        LocalDate oggi = LocalDate.now();

        Hackathon hackathon = Hackathon.crea(
                new DatiHackathon(
                        "HackHub 2026",
                        "Regolamento",
                        "Qualità, completezza e innovazione",
                        oggi.minusDays(20),
                        oggi.minusDays(15),
                        oggi.minusDays(10),
                        "Camerino",
                        BigDecimal.valueOf(5000),
                        5
                ),
                organizzatore,
                new Utente(2L),
                List.of(new Utente(3L))
        );

        hackathon.aggiornaStato(oggi);
        return hackathon;
    }

    private Partecipazione creaPartecipazione(
            Hackathon hackathon,
            long responsabileId
    ) {
        Utente responsabile = new Utente(
                responsabileId
        );

        Team team = Team.crea(
                "Team " + responsabileId,
                responsabile,
                responsabile
        );

        return new Partecipazione(
                hackathon,
                team
        );
    }

    private Partecipazione creaPartecipazioneValutata(
            Hackathon hackathon,
            long responsabileId
    ) {
        Partecipazione partecipazione =
                creaPartecipazione(
                        hackathon,
                        responsabileId
                );

        Sottomissione sottomissione =
                new Sottomissione(
                        partecipazione,
                        "Sottomissione del team "
                                + responsabileId
                );

        Valutazione.crea(
                sottomissione,
                hackathon.getGiudice(),
                new DatiValutazione(
                        "Buona sottomissione",
                        BigDecimal.valueOf(8)
                )
        );

        return partecipazione;
    }
}