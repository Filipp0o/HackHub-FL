package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.DatiDecisioneSegnalazione;
import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.EsitoSegnalazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegnalazioneRepositoryImplTest {

    @Test
    void rifiutaOrganizzatoreNullo() {
        SegnalazioneRepositoryImpl repository =
                new SegnalazioneRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository
                        .ottieniSegnalazioniDaEsaminare(null)
        );
    }

    @Test
    void rifiutaSegnalazioneNullaDuranteIlSalvataggio() {
        SegnalazioneRepositoryImpl repository =
                new SegnalazioneRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
    }

    @Test
    void rifiutaParametriNulliNelSalvataggioConNotifica() {
        SegnalazioneRepositoryImpl repository =
                new SegnalazioneRepositoryImpl();

        Utente organizzatore = new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(
                        organizzatore,
                        new Utente(3L),
                        10L
                );

        NotificaSegnalazione notifica =
                NotificaSegnalazione.crea(
                        segnalazione,
                        organizzatore
                );

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> repository.salvaConNotifica(
                                null,
                                notifica
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> repository.salvaConNotifica(
                                segnalazione,
                                null
                        )
                )
        );
    }

    @Test
    void rifiutaNotificaNullaDuranteIlSalvataggio() {
        SegnalazioneRepositoryImpl repository =
                new SegnalazioneRepositoryImpl();

        assertThrows(
                NullPointerException.class,
                () -> repository.salvaNotifica(null)
        );
    }

    @Test
    void restituisceSoloSegnalazioniDaEsaminareDellOrganizzatore() {
        SegnalazioneRepositoryImpl repository =
                new SegnalazioneRepositoryImpl();

        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        Segnalazione daEsaminare =
                creaSegnalazione(
                        organizzatore,
                        mentore,
                        10L
                );

        Segnalazione giaEsaminata =
                creaSegnalazione(
                        organizzatore,
                        mentore,
                        20L
                );

        giaEsaminata.registraEsame(
                new DatiDecisioneSegnalazione(
                        EsitoSegnalazione.ARCHIVIATA,
                        "Segnalazione non confermata"
                ),
                organizzatore
        );

        Segnalazione diAltroOrganizzatore =
                creaSegnalazione(
                        new Utente(4L),
                        mentore,
                        30L
                );

        repository.salva(daEsaminare);
        repository.salva(giaEsaminata);
        repository.salva(diAltroOrganizzatore);

        assertEquals(
                List.of(daEsaminare),
                repository.ottieniSegnalazioniDaEsaminare(
                        new Utente(1L)
                )
        );
    }

    @Test
    void salvaAtomicamenteSegnalazioneENotifica() {
        SegnalazioneRepositoryImpl repository =
                new SegnalazioneRepositoryImpl();

        Utente organizzatore = new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(
                        organizzatore,
                        new Utente(3L),
                        10L
                );

        NotificaSegnalazione notifica =
                NotificaSegnalazione.crea(
                        segnalazione,
                        organizzatore
                );

        repository.salvaConNotifica(
                segnalazione,
                notifica
        );

        assertEquals(
                List.of(segnalazione),
                repository.ottieniSegnalazioniDaEsaminare(
                        organizzatore
                )
        );
    }

    @Test
    void rifiutaNotificaRiferitaAUnAltraSegnalazione() {
        SegnalazioneRepositoryImpl repository =
                new SegnalazioneRepositoryImpl();

        Utente organizzatore = new Utente(1L);
        Utente mentore = new Utente(3L);

        Segnalazione primaSegnalazione =
                creaSegnalazione(
                        organizzatore,
                        mentore,
                        10L
                );

        Segnalazione secondaSegnalazione =
                creaSegnalazione(
                        organizzatore,
                        mentore,
                        20L
                );

        NotificaSegnalazione notificaSeconda =
                NotificaSegnalazione.crea(
                        secondaSegnalazione,
                        organizzatore
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.salvaConNotifica(
                        primaSegnalazione,
                        notificaSeconda
                )
        );
    }

    @Test
    void restituisceListaNonModificabile() {
        SegnalazioneRepositoryImpl repository =
                new SegnalazioneRepositoryImpl();

        Utente organizzatore = new Utente(1L);

        Segnalazione segnalazione =
                creaSegnalazione(
                        organizzatore,
                        new Utente(3L),
                        10L
                );

        repository.salva(segnalazione);

        List<Segnalazione> risultato =
                repository.ottieniSegnalazioniDaEsaminare(
                        organizzatore
                );

        assertThrows(
                UnsupportedOperationException.class,
                () -> risultato.add(segnalazione)
        );
    }

    private Segnalazione creaSegnalazione(
            Utente organizzatore,
            Utente mentore,
            Long idResponsabile
    ) {
        Hackathon hackathon =
                creaHackathonInCorso(
                        organizzatore,
                        mentore
                );

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

        return Segnalazione.crea(
                mentore,
                partecipazione,
                "Violazione del regolamento"
        );
    }

    private Hackathon creaHackathonInCorso(
            Utente organizzatore,
            Utente mentore
    ) {
        LocalDate oggi = LocalDate.now();

        DatiHackathon dati = new DatiHackathon(
                "Hackathon",
                "Regolamento",
                "Criteri di valutazione",
                oggi.minusDays(2),
                oggi.minusDays(1),
                oggi.plusDays(1),
                "Camerino",
                BigDecimal.valueOf(1_000),
                5
        );

        Hackathon hackathon = Hackathon.crea(
                dati,
                organizzatore,
                new Utente(2L),
                List.of(mentore)
        );

        hackathon.aggiornaStato(oggi);
        return hackathon;
    }
}