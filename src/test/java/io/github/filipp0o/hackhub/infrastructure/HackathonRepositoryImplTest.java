package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.DatiValutazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Valutazione;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HackathonRepositoryImplTest {

    @Test
    void rifiutaRepositoryPartecipazioniNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new HackathonRepositoryImpl(null)
        );
    }

    @Test
    void rifiutaGiudiceNullo() {
        HackathonRepositoryImpl repository =
                creaRepository();

        assertThrows(
                NullPointerException.class,
                () -> repository.ottieniHackathonValutabili(null)
        );
    }

    @Test
    void rifiutaMentoreNullo() {
        HackathonRepositoryImpl repository =
                creaRepository();

        assertThrows(
                NullPointerException.class,
                () -> repository.ottieniHackathonSegnalabili(null)
        );
    }

    @Test
    void rifiutaHackathonNulloDuranteIlSalvataggio() {
        HackathonRepositoryImpl repository =
                creaRepository();

        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
    }

    @Test
    void restituisceSoloHackathonValutabiliConSottomissioniPendenti() {
        PartecipazioneRepositoryImpl partecipazioneRepository =
                new PartecipazioneRepositoryImpl();

        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl(
                        partecipazioneRepository
                );

        Utente giudice = new Utente(2L);
        List<Utente> mentori =
                List.of(new Utente(3L));

        Hackathon valutabile =
                creaHackathonInValutazione(
                        "Hackathon valutabile",
                        giudice,
                        mentori
                );

        salvaPartecipazione(
                partecipazioneRepository,
                valutabile,
                10L,
                true
        );

        Hackathon senzaSottomissioni =
                creaHackathonInValutazione(
                        "Senza sottomissioni",
                        giudice,
                        mentori
                );

        salvaPartecipazione(
                partecipazioneRepository,
                senzaSottomissioni,
                20L,
                false
        );

        Hackathon conSottomissioneGiaValutata =
                creaHackathonInValutazione(
                        "Già valutato",
                        giudice,
                        mentori
                );

        Partecipazione partecipazioneValutata =
                salvaPartecipazione(
                        partecipazioneRepository,
                        conSottomissioneGiaValutata,
                        30L,
                        true
                );

        Valutazione.crea(
                partecipazioneValutata.getSottomissione(),
                giudice,
                new DatiValutazione(
                        "Buona sottomissione",
                        BigDecimal.valueOf(8)
                )
        );

        Hackathon inCorso =
                creaHackathonInCorso(
                        "Hackathon in corso",
                        giudice,
                        mentori
                );

        salvaPartecipazione(
                partecipazioneRepository,
                inCorso,
                40L,
                true
        );

        Hackathon assegnatoAdAltroGiudice =
                creaHackathonInValutazione(
                        "Altro giudice",
                        new Utente(4L),
                        mentori
                );

        salvaPartecipazione(
                partecipazioneRepository,
                assegnatoAdAltroGiudice,
                50L,
                true
        );

        repository.salva(valutabile);
        repository.salva(senzaSottomissioni);
        repository.salva(conSottomissioneGiaValutata);
        repository.salva(inCorso);
        repository.salva(assegnatoAdAltroGiudice);

        assertEquals(
                List.of(valutabile),
                repository.ottieniHackathonValutabili(
                        giudice
                )
        );
    }

    @Test
    void riconosceGiudiceTramiteIdentificativo() {
        PartecipazioneRepositoryImpl partecipazioneRepository =
                new PartecipazioneRepositoryImpl();

        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl(
                        partecipazioneRepository
                );

        Hackathon hackathon =
                creaHackathonInValutazione(
                        "Hackathon",
                        new Utente(2L),
                        List.of(new Utente(3L))
                );

        salvaPartecipazione(
                partecipazioneRepository,
                hackathon,
                10L,
                true
        );

        repository.salva(hackathon);

        assertEquals(
                List.of(hackathon),
                repository.ottieniHackathonValutabili(
                        new Utente(2L)
                )
        );
    }

    @Test
    void restituisceSoloHackathonSegnalabiliConTeamIscritti() {
        PartecipazioneRepositoryImpl partecipazioneRepository =
                new PartecipazioneRepositoryImpl();

        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl(
                        partecipazioneRepository
                );

        Utente mentore = new Utente(3L);
        Utente giudice = new Utente(2L);

        Hackathon inCorso =
                creaHackathonInCorso(
                        "Hackathon in corso",
                        giudice,
                        List.of(mentore)
                );

        salvaPartecipazione(
                partecipazioneRepository,
                inCorso,
                10L,
                false
        );

        Hackathon inValutazione =
                creaHackathonInValutazione(
                        "Hackathon in valutazione",
                        giudice,
                        List.of(mentore)
                );

        salvaPartecipazione(
                partecipazioneRepository,
                inValutazione,
                20L,
                false
        );

        Hackathon inIscrizione =
                creaHackathonInIscrizione(
                        "Hackathon in iscrizione",
                        giudice,
                        List.of(mentore)
                );

        salvaPartecipazione(
                partecipazioneRepository,
                inIscrizione,
                30L,
                false
        );

        Hackathon senzaTeam =
                creaHackathonInCorso(
                        "Hackathon senza team",
                        giudice,
                        List.of(mentore)
                );

        Hackathon assegnatoAdAltroMentore =
                creaHackathonInCorso(
                        "Altro mentore",
                        giudice,
                        List.of(new Utente(4L))
                );

        salvaPartecipazione(
                partecipazioneRepository,
                assegnatoAdAltroMentore,
                40L,
                false
        );

        repository.salva(inCorso);
        repository.salva(inValutazione);
        repository.salva(inIscrizione);
        repository.salva(senzaTeam);
        repository.salva(assegnatoAdAltroMentore);

        assertEquals(
                List.of(inCorso, inValutazione),
                repository.ottieniHackathonSegnalabili(
                        new Utente(3L)
                )
        );
    }

    @Test
    void restituisceListeNonModificabili() {
        PartecipazioneRepositoryImpl partecipazioneRepository =
                new PartecipazioneRepositoryImpl();

        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl(
                        partecipazioneRepository
                );

        Utente giudice = new Utente(2L);
        Utente mentore = new Utente(3L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        "Hackathon",
                        giudice,
                        List.of(mentore)
                );

        salvaPartecipazione(
                partecipazioneRepository,
                hackathon,
                10L,
                true
        );

        repository.salva(hackathon);

        List<Hackathon> valutabili =
                repository.ottieniHackathonValutabili(
                        giudice
                );

        List<Hackathon> segnalabili =
                repository.ottieniHackathonSegnalabili(
                        mentore
                );

        assertAll(
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> valutabili.add(hackathon)
                ),
                () -> assertThrows(
                        UnsupportedOperationException.class,
                        () -> segnalabili.add(hackathon)
                )
        );
    }

    private HackathonRepositoryImpl creaRepository() {
        return new HackathonRepositoryImpl(
                new PartecipazioneRepositoryImpl()
        );
    }

    private Partecipazione salvaPartecipazione(
            PartecipazioneRepositoryImpl repository,
            Hackathon hackathon,
            Long idResponsabile,
            boolean conSottomissione
    ) {
        Utente responsabile =
                new Utente(idResponsabile);

        Team team = Team.crea(
                "Team " + idResponsabile,
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                new Partecipazione(
                        hackathon,
                        team
                );

        if (conSottomissione) {
            new Sottomissione(
                    partecipazione,
                    "Contenuto della sottomissione"
            );
        }

        repository.salva(partecipazione);
        return partecipazione;
    }

    private Hackathon creaHackathonInIscrizione(
            String nome,
            Utente giudice,
            List<Utente> mentori
    ) {
        LocalDate oggi = LocalDate.now();

        return creaHackathon(
                nome,
                giudice,
                mentori,
                oggi.plusDays(2),
                oggi.plusDays(5)
        );
    }

    private Hackathon creaHackathonInCorso(
            String nome,
            Utente giudice,
            List<Utente> mentori
    ) {
        LocalDate oggi = LocalDate.now();

        Hackathon hackathon = creaHackathon(
                nome,
                giudice,
                mentori,
                oggi.minusDays(1),
                oggi.plusDays(1)
        );

        hackathon.aggiornaStato(oggi);
        return hackathon;
    }

    private Hackathon creaHackathonInValutazione(
            String nome,
            Utente giudice,
            List<Utente> mentori
    ) {
        LocalDate oggi = LocalDate.now();

        Hackathon hackathon = creaHackathon(
                nome,
                giudice,
                mentori,
                oggi.minusDays(5),
                oggi.minusDays(1)
        );

        hackathon.aggiornaStato(oggi);
        return hackathon;
    }

    private Hackathon creaHackathon(
            String nome,
            Utente giudice,
            List<Utente> mentori,
            LocalDate dataInizio,
            LocalDate dataFine
    ) {
        DatiHackathon dati = new DatiHackathon(
                nome,
                "Regolamento",
                "Criteri di valutazione",
                dataInizio.minusDays(1),
                dataInizio,
                dataFine,
                "Camerino",
                BigDecimal.valueOf(1_000),
                5
        );

        return Hackathon.crea(
                dati,
                new Utente(1L),
                giudice,
                mentori
        );
    }
}