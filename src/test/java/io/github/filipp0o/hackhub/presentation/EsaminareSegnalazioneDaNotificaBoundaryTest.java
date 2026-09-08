package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.EsaminareSegnalazioneControl;
import io.github.filipp0o.hackhub.domain.*;
import io.github.filipp0o.hackhub.infrastructure.InMemoryPartecipazioneRepository;
import io.github.filipp0o.hackhub.infrastructure.SegnalazioneRepositoryImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class EsaminareSegnalazioneDaNotificaBoundaryTest {

    @Test
    void selezionaNotificaEMostraDatiRichiestiDalCasoUso() {
        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        Hackathon hackathon = creaHackathonInCorso(
                organizzatore,
                mentore
        );

        Utente responsabile = new Utente(4L);
        Team team = Team.crea("Team Alpha", responsabile, responsabile);

        Partecipazione partecipazione =
                new Partecipazione(hackathon, team);

        Segnalazione segnalazione = Segnalazione.crea(
                mentore,
                partecipazione,
                "Uso di materiale non consentito"
        );

        NotificaSegnalazione notifica = NotificaSegnalazione.crea(
                segnalazione,
                organizzatore
        );

        EsaminareSegnalazioneControl control =
                new EsaminareSegnalazioneControl(
                        new SegnalazioneRepositoryImpl(),
                        new InMemoryPartecipazioneRepository()
                );

        SessioneUtente sessione = new SessioneUtente();
        sessione.registra(organizzatore);

        EsaminareSegnalazioneBoundary boundary =
                new EsaminareSegnalazioneBoundary(control, sessione);

        EsaminareSegnalazioneBoundary.RiepilogoSegnalazione risultato =
                boundary.selezionaNotificaSegnalazione(notifica);

        assertAll(
                () -> assertEquals(segnalazione.getId(), risultato.id()),
                () -> assertEquals(
                        "Uso di materiale non consentito",
                        risultato.descrizione()
                ),
                () -> assertEquals("Team Alpha", risultato.nomeTeam()),
                () -> assertEquals(
                        "Regolamento ufficiale",
                        risultato.regolamento()
                ),
                () -> assertEquals(
                        List.of(EsitoSegnalazione.values()),
                        risultato.esitiDisponibili()
                ),
                () -> assertTrue(notifica.getLetta())
        );
    }

    private Hackathon creaHackathonInCorso(
            Utente organizzatore,
            Utente mentore
    ) {
        LocalDate oggi = LocalDate.now();

        Hackathon hackathon = Hackathon.crea(
                new DatiHackathon(
                        "HackHub 2026",
                        "Regolamento ufficiale",
                        "Criteri di valutazione",
                        oggi.minusDays(5),
                        oggi.minusDays(2),
                        oggi.plusDays(2),
                        "Camerino",
                        BigDecimal.valueOf(5000),
                        5
                ),
                organizzatore,
                new Utente(2L),
                List.of(mentore)
        );

        hackathon.aggiornaStato(oggi);
        return hackathon;
    }
}