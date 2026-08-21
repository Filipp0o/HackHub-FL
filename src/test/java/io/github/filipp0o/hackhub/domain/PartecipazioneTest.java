package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PartecipazioneTest {

    private final Utente responsabile = new Utente(1L);

    @Test
    void creaPartecipazioneValidaSenzaSottomissione() {
        Hackathon hackathon = creaHackathonValido();
        Team team = creaTeamValido();

        Partecipazione partecipazione =
                new Partecipazione(hackathon, team);

        assertAll(
                () -> assertEquals(
                        StatoPartecipazione.ATTIVA,
                        partecipazione.getStato()
                ),
                () -> assertSame(
                        hackathon,
                        partecipazione.getHackathon()
                ),
                () -> assertSame(
                        team,
                        partecipazione.getTeam()
                ),
                () -> assertNull(
                        partecipazione.getSottomissione()
                )
        );
    }

    @Test
    void creaPartecipazioneValidaConSottomissione() {
        Hackathon hackathon = creaHackathonValido();
        Team team = creaTeamValido();

        Partecipazione partecipazione =
                new Partecipazione(hackathon, team);

        Sottomissione sottomissione =
                new Sottomissione(
                        partecipazione,
                        "Repository del progetto"
                );

        assertAll(
                () -> assertEquals(
                        StatoPartecipazione.ATTIVA,
                        partecipazione.getStato()
                ),
                () -> assertSame(
                        hackathon,
                        partecipazione.getHackathon()
                ),
                () -> assertSame(
                        team,
                        partecipazione.getTeam()
                ),
                () -> assertSame(
                        sottomissione,
                        partecipazione.getSottomissione()
                ),
                () -> assertSame(
                        partecipazione,
                        sottomissione.getPartecipazione()
                )
        );
    }

    @Test
    void rifiutaHackathonNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new Partecipazione(
                        null,
                        creaTeamValido()
                )
        );
    }

    @Test
    void rifiutaTeamNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new Partecipazione(
                        creaHackathonValido(),
                        null
                )
        );
    }

    @Test
    void ottieniHackathonRestituisceHackathonAssociato() {
        Hackathon hackathon = creaHackathonValido();
        Team team = creaTeamValido();

        Partecipazione partecipazione =
                Partecipazione.crea(
                        hackathon,
                        team
                );

        assertSame(
                hackathon,
                partecipazione.ottieniHackathon()
        );
    }

    @Test
    void escludiImpostaLoStatoEsclusa() {
        Partecipazione partecipazione =
                new Partecipazione(
                        creaHackathonValido(),
                        creaTeamValido()
                );

        partecipazione.escludi();

        assertEquals(
                StatoPartecipazione.ESCLUSA,
                partecipazione.getStato()
        );
    }

    private Team creaTeamValido() {
        return Team.crea(
                "Team Alpha",
                responsabile,
                responsabile
        );
    }

    private Hackathon creaHackathonValido() {
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
                new Utente(2L),
                new Utente(3L),
                List.of(new Utente(4L))
        );
    }
}