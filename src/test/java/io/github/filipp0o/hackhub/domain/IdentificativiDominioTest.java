package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IdentificativiDominioTest {

    @Test
    void assegnaIdentificativiDistintiAgliHackathon() {
        Hackathon primoHackathon = creaHackathon();
        Hackathon secondoHackathon = creaHackathon();

        assertAll(
                () -> assertNotNull(primoHackathon.getId()),
                () -> assertNotNull(secondoHackathon.getId()),
                () -> assertTrue(primoHackathon.getId() > 0),
                () -> assertTrue(secondoHackathon.getId() > 0),
                () -> assertNotEquals(
                        primoHackathon.getId(),
                        secondoHackathon.getId()
                )
        );
    }

    @Test
    void assegnaIdentificativiDistintiAlleSottomissioni() {
        Sottomissione primaSottomissione =
                creaSottomissione("Prima sottomissione");

        Sottomissione secondaSottomissione =
                creaSottomissione("Seconda sottomissione");

        assertAll(
                () -> assertNotNull(primaSottomissione.getId()),
                () -> assertNotNull(secondaSottomissione.getId()),
                () -> assertTrue(primaSottomissione.getId() > 0),
                () -> assertTrue(secondaSottomissione.getId() > 0),
                () -> assertNotEquals(
                        primaSottomissione.getId(),
                        secondaSottomissione.getId()
                )
        );
    }

    private Sottomissione creaSottomissione(
            String contenuto
    ) {
        Hackathon hackathon = creaHackathon();
        Utente responsabile = new Utente(4L);

        Team team = Team.crea(
                "Team Alpha",
                responsabile,
                responsabile
        );

        Partecipazione partecipazione =
                new Partecipazione(hackathon, team);

        return new Sottomissione(
                partecipazione,
                contenuto
        );
    }

    private Hackathon creaHackathon() {
        DatiHackathon dati = new DatiHackathon(
                "HackHub 2026",
                "Regolamento ufficiale",
                "Criteri di valutazione",
                LocalDate.of(2026, 10, 1),
                LocalDate.of(2026, 10, 10),
                LocalDate.of(2026, 10, 12),
                "Camerino",
                BigDecimal.valueOf(5000),
                5
        );

        return Hackathon.crea(
                dati,
                new Utente(1L),
                new Utente(2L),
                List.of(new Utente(3L))
        );
    }
}