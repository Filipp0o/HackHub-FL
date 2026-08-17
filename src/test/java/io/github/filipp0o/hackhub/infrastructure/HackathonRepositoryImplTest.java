package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class HackathonRepositoryImplTest {

    @Test
    void rifiutaGiudiceNullo() {
        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.ottieniHackathonValutabili(null)
        );
    }

    @Test
    void rifiutaMentoreNullo() {
        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.ottieniHackathonSegnalabili(null)
        );
    }

    @Test
    void rifiutaHackathonNulloDuranteIlSalvataggio() {
        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
    }

    @Test
    void restituisceSoloHackathonInValutazioneAssegnatiAlGiudice() {
        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl();

        Utente giudice = new Utente(2L);

        Hackathon valutabile = creaHackathonInValutazione(
                "Hackathon valutabile",
                giudice,
                List.of(new Utente(3L))
        );

        Hackathon inCorso = creaHackathonInCorso(
                "Hackathon in corso",
                giudice,
                List.of(new Utente(3L))
        );

        Hackathon assegnatoAdAltroGiudice =
                creaHackathonInValutazione(
                        "Altro hackathon",
                        new Utente(4L),
                        List.of(new Utente(3L))
                );

        repository.salva(valutabile);
        repository.salva(inCorso);
        repository.salva(assegnatoAdAltroGiudice);

        assertEquals(
                List.of(valutabile),
                repository.ottieniHackathonValutabili(giudice)
        );
    }

    @Test
    void riconosceGiudiceTramiteIdentificativo() {
        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl();

        Hackathon hackathon = creaHackathonInValutazione(
                "Hackathon",
                new Utente(2L),
                List.of(new Utente(3L))
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
    void restituisceSoloHackathonSegnalabiliAssegnatiAlMentore() {
        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl();

        Utente mentore = new Utente(3L);

        Hackathon inCorso = creaHackathonInCorso(
                "Hackathon in corso",
                new Utente(2L),
                List.of(mentore)
        );

        Hackathon inValutazione = creaHackathonInValutazione(
                "Hackathon in valutazione",
                new Utente(2L),
                List.of(mentore)
        );

        Hackathon inIscrizione = creaHackathonInIscrizione(
                "Hackathon in iscrizione",
                new Utente(2L),
                List.of(mentore)
        );

        Hackathon assegnatoAdAltroMentore =
                creaHackathonInCorso(
                        "Altro hackathon",
                        new Utente(2L),
                        List.of(new Utente(4L))
                );

        repository.salva(inCorso);
        repository.salva(inValutazione);
        repository.salva(inIscrizione);
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
        HackathonRepositoryImpl repository =
                new HackathonRepositoryImpl();

        Utente giudice = new Utente(2L);
        Utente mentore = new Utente(3L);

        Hackathon hackathon = creaHackathonInValutazione(
                "Hackathon",
                giudice,
                List.of(mentore)
        );

        repository.salva(hackathon);

        List<Hackathon> valutabili =
                repository.ottieniHackathonValutabili(giudice);

        List<Hackathon> segnalabili =
                repository.ottieniHackathonSegnalabili(mentore);

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