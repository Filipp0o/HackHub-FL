package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NotificaSegnalazioneTest {

    private final Utente mentore = new Utente(1L);
    private final Utente destinatario = new Utente(2L);

    @Test
    void creaNotificaNonLettaECollegaSegnalazione() {
        Segnalazione segnalazione =
                creaSegnalazioneValida();

        assertNull(
                segnalazione.getNotificaSegnalazione()
        );

        NotificaSegnalazione notifica =
                NotificaSegnalazione.crea(
                        segnalazione,
                        destinatario
                );

        assertAll(
                () -> assertSame(
                        segnalazione,
                        notifica.getSegnalazione()
                ),
                () -> assertSame(
                        notifica,
                        segnalazione.getNotificaSegnalazione()
                ),
                () -> assertSame(
                        destinatario,
                        notifica.getDestinatario()
                ),
                () -> assertNotNull(
                        notifica.getDataOraCreazione()
                ),
                () -> assertFalse(
                        notifica.getLetta()
                )
        );
    }

    @Test
    void rifiutaSegnalazioneNulla() {
        assertThrows(
                NullPointerException.class,
                () -> NotificaSegnalazione.crea(
                        null,
                        destinatario
                )
        );
    }

    @Test
    void rifiutaDestinatarioNullo() {
        Segnalazione segnalazione =
                creaSegnalazioneValida();

        assertThrows(
                NullPointerException.class,
                () -> NotificaSegnalazione.crea(
                        segnalazione,
                        null
                )
        );

        assertNull(
                segnalazione.getNotificaSegnalazione()
        );
    }

    @Test
    void impedisceSecondaNotificaDellaStessaSegnalazione() {
        Segnalazione segnalazione =
                creaSegnalazioneValida();

        NotificaSegnalazione primaNotifica =
                NotificaSegnalazione.crea(
                        segnalazione,
                        destinatario
                );

        assertThrows(
                IllegalStateException.class,
                () -> NotificaSegnalazione.crea(
                        segnalazione,
                        new Utente(6L)
                )
        );

        assertSame(
                primaNotifica,
                segnalazione.getNotificaSegnalazione()
        );
    }

    @Test
    void segnaNotificaComeLetta() {
        NotificaSegnalazione notifica =
                NotificaSegnalazione.crea(
                        creaSegnalazioneValida(),
                        destinatario
                );

        assertFalse(notifica.getLetta());

        notifica.segnaComeLetta();

        assertTrue(notifica.getLetta());
    }

    private Segnalazione creaSegnalazioneValida() {
        Hackathon hackathon =
                creaHackathonValido();

        Team team =
                creaTeamValido();

        Partecipazione partecipazione =
                new Partecipazione(
                        hackathon,
                        team
                );

        return Segnalazione.crea(
                mentore,
                partecipazione,
                "Violazione del regolamento"
        );
    }

    private Team creaTeamValido() {
        Utente responsabile = new Utente(5L);

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

        Hackathon hackathon = Hackathon.crea(
                dati,
                new Utente(3L),
                new Utente(4L),
                List.of(mentore)
        );

        hackathon.aggiornaStato(dati.dataInizio());

        return hackathon;
    }
}