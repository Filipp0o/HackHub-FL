package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SegnalazioneTest {

    private final Utente mentore = new Utente(1L);
    private final Utente esaminatore = new Utente(2L);

    private final Hackathon hackathon = creaHackathonValido();
    private final Team team = creaTeamValido();

    private final Partecipazione partecipazione =
            new Partecipazione(hackathon, team);

    @Test
    void creaSegnalazioneDaEsaminare() {
        Segnalazione segnalazione = creaSegnalazioneValida();

        assertAll(
                () -> assertEquals(
                        "Violazione del regolamento",
                        segnalazione.getDescrizione()
                ),
                () -> assertEquals(
                        mentore,
                        segnalazione.getMentoreSegnalante()
                ),
                () -> assertSame(
                        partecipazione,
                        segnalazione.getPartecipazione()
                ),
                () -> assertEquals(
                        StatoSegnalazione.DA_ESAMINARE,
                        segnalazione.getStato()
                ),
                () -> assertNotNull(
                        segnalazione.getDataOraCreazione()
                ),
                () -> assertNull(segnalazione.getEsito()),
                () -> assertNull(segnalazione.getMotivazione()),
                () -> assertNull(segnalazione.getEsaminatore()),
                () -> assertNull(segnalazione.getDataOraEsame())
        );
    }

    @Test
    void rifiutaMentoreSegnalanteNullo() {
        assertThrows(
                NullPointerException.class,
                () -> Segnalazione.crea(
                        null,
                        partecipazione,
                        "Violazione del regolamento"
                )
        );
    }

    @Test
    void rifiutaPartecipazioneNulla() {
        assertThrows(
                NullPointerException.class,
                () -> Segnalazione.crea(
                        mentore,
                        null,
                        "Violazione del regolamento"
                )
        );
    }

    @Test
    void rifiutaDescrizioneNulla() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Segnalazione.crea(
                        mentore,
                        partecipazione,
                        null
                )
        );
    }

    @Test
    void rifiutaDescrizioneVuota() {
        assertThrows(
                IllegalArgumentException.class,
                () -> Segnalazione.crea(
                        mentore,
                        partecipazione,
                        "   "
                )
        );
    }

    @Test
    void registraEsameValido() {
        Segnalazione segnalazione = creaSegnalazioneValida();

        DatiDecisioneSegnalazione dati =
                new DatiDecisioneSegnalazione(
                        EsitoSegnalazione.VIOLAZIONE_CON_ESCLUSIONE,
                        "La violazione è stata confermata"
                );

        segnalazione.registraEsame(dati, esaminatore);

        assertAll(
                () -> assertEquals(
                        StatoSegnalazione.ESAMINATA,
                        segnalazione.getStato()
                ),
                () -> assertEquals(
                        dati.esito(),
                        segnalazione.getEsito()
                ),
                () -> assertEquals(
                        dati.motivazione(),
                        segnalazione.getMotivazione()
                ),
                () -> assertEquals(
                        esaminatore,
                        segnalazione.getEsaminatore()
                ),
                () -> assertNotNull(
                        segnalazione.getDataOraEsame()
                ),
                () -> assertEquals(
                        StatoPartecipazione.ATTIVA,
                        partecipazione.getStato()
                )
        );
    }

    @Test
    void rifiutaDatiDecisioneNulli() {
        Segnalazione segnalazione = creaSegnalazioneValida();

        assertThrows(
                NullPointerException.class,
                () -> segnalazione.registraEsame(
                        null,
                        esaminatore
                )
        );
    }

    @Test
    void rifiutaEsitoNullo() {
        Segnalazione segnalazione = creaSegnalazioneValida();

        DatiDecisioneSegnalazione dati =
                new DatiDecisioneSegnalazione(
                        null,
                        "Motivazione valida"
                );

        assertThrows(
                NullPointerException.class,
                () -> segnalazione.registraEsame(
                        dati,
                        esaminatore
                )
        );
    }

    @Test
    void rifiutaMotivazioneNullaOVuota() {
        for (String motivazione : new String[]{null, "   "}) {
            Segnalazione segnalazione =
                    creaSegnalazioneValida();

            DatiDecisioneSegnalazione dati =
                    new DatiDecisioneSegnalazione(
                            EsitoSegnalazione
                                    .VIOLAZIONE_CON_ESCLUSIONE,
                            motivazione
                    );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> segnalazione.registraEsame(
                            dati,
                            esaminatore
                    )
            );
        }
    }

    @Test
    void rifiutaEsaminatoreNullo() {
        Segnalazione segnalazione = creaSegnalazioneValida();

        assertThrows(
                NullPointerException.class,
                () -> segnalazione.registraEsame(
                        datiDecisioneValidi(),
                        null
                )
        );
    }

    @Test
    void impedisceUnSecondoEsame() {
        Segnalazione segnalazione = creaSegnalazioneValida();

        segnalazione.registraEsame(
                datiDecisioneValidi(),
                esaminatore
        );

        assertThrows(
                IllegalStateException.class,
                () -> segnalazione.registraEsame(
                        datiDecisioneValidi(),
                        esaminatore
                )
        );
    }

    private Segnalazione creaSegnalazioneValida() {
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

        return Hackathon.crea(
                dati,
                new Utente(3L),
                new Utente(4L),
                List.of(mentore)
        );
    }

    private DatiDecisioneSegnalazione datiDecisioneValidi() {
        return new DatiDecisioneSegnalazione(
                EsitoSegnalazione.VIOLAZIONE_CON_ESCLUSIONE,
                "La violazione è stata confermata"
        );
    }
}