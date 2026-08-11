package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SottomissioneTest {

    @Test
    void creaSottomissioneValidaECollegaLaPartecipazione() {
        Partecipazione partecipazione =
                creaPartecipazioneValida();

        Sottomissione sottomissione = new Sottomissione(
                partecipazione,
                "Repository del progetto"
        );

        assertAll(
                () -> assertEquals(
                        "Repository del progetto",
                        sottomissione.getContenuto()
                ),
                () -> assertSame(
                        partecipazione,
                        sottomissione.getPartecipazione()
                ),
                () -> assertSame(
                        sottomissione,
                        partecipazione.getSottomissione()
                ),
                () -> assertNull(
                        sottomissione.getValutazione()
                )
        );
    }

    @Test
    void rifiutaPartecipazioneNulla() {
        assertThrows(
                NullPointerException.class,
                () -> new Sottomissione(
                        null,
                        "Repository del progetto"
                )
        );
    }

    @Test
    void rifiutaContenutoNullo() {
        Partecipazione partecipazione =
                creaPartecipazioneValida();

        assertThrows(
                IllegalArgumentException.class,
                () -> new Sottomissione(
                        partecipazione,
                        null
                )
        );

        assertNull(partecipazione.getSottomissione());
    }

    @Test
    void rifiutaContenutoVuoto() {
        Partecipazione partecipazione =
                creaPartecipazioneValida();

        assertThrows(
                IllegalArgumentException.class,
                () -> new Sottomissione(
                        partecipazione,
                        "   "
                )
        );

        assertNull(partecipazione.getSottomissione());
    }

    @Test
    void impedisceSecondaSottomissioneDellaStessaPartecipazione() {
        Partecipazione partecipazione =
                creaPartecipazioneValida();

        Sottomissione primaSottomissione =
                new Sottomissione(
                        partecipazione,
                        "Prima versione del progetto"
                );

        assertThrows(
                IllegalStateException.class,
                () -> new Sottomissione(
                        partecipazione,
                        "Seconda versione del progetto"
                )
        );

        assertSame(
                primaSottomissione,
                partecipazione.getSottomissione()
        );
    }

    private Partecipazione creaPartecipazioneValida() {
        Hackathon hackathon = creaHackathonValido();

        Utente responsabile = new Utente(4L);

        Team team = Team.crea(
                "Team Alpha",
                responsabile,
                responsabile
        );

        return new Partecipazione(hackathon, team);
    }

    private Hackathon creaHackathonValido() {
        Utente organizzatore = new Utente(1L);
        Utente giudice = new Utente(2L);
        Utente mentore = new Utente(3L);

        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Criteri di valutazione",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12),
                "Roma",
                BigDecimal.valueOf(5000),
                5
        );

        return Hackathon.crea(
                dati,
                organizzatore,
                giudice,
                List.of(mentore)
        );
    }
}