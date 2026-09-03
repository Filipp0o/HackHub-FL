package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class StatoHackathonTest {

    private static final LocalDate DATA_INIZIO =
            LocalDate.of(2026, 10, 10);

    private static final LocalDate DATA_FINE =
            LocalDate.of(2026, 10, 12);

    @Test
    void factoryRicostruisceOgniTipoDiStato() {
        assertAll(
                () -> assertEquals(
                        TipoStatoHackathon.IN_ISCRIZIONE,
                        StatoHackathonFactory
                                .ricostruisci(
                                        TipoStatoHackathon.IN_ISCRIZIONE
                                )
                                .tipo()
                ),
                () -> assertEquals(
                        TipoStatoHackathon.IN_CORSO,
                        StatoHackathonFactory
                                .ricostruisci(
                                        TipoStatoHackathon.IN_CORSO
                                )
                                .tipo()
                ),
                () -> assertEquals(
                        TipoStatoHackathon.IN_VALUTAZIONE,
                        StatoHackathonFactory
                                .ricostruisci(
                                        TipoStatoHackathon.IN_VALUTAZIONE
                                )
                                .tipo()
                ),
                () -> assertEquals(
                        TipoStatoHackathon.CONCLUSO,
                        StatoHackathonFactory
                                .ricostruisci(
                                        TipoStatoHackathon.CONCLUSO
                                )
                                .tipo()
                )
        );
    }

    @Test
    void factoryRifiutaTipoNullo() {
        assertThrows(
                NullPointerException.class,
                () -> StatoHackathonFactory.ricostruisci(null)
        );
    }

    @Test
    void statoInIscrizioneGestisceLeTransizioniTemporali() {
        StatoHackathon stato =
                new StatoHackathonInIscrizione();

        assertAll(
                () -> assertEquals(
                        TipoStatoHackathon.IN_ISCRIZIONE,
                        stato.aggiorna(
                                DATA_INIZIO.minusDays(1),
                                DATA_INIZIO,
                                DATA_FINE
                        ).tipo()
                ),
                () -> assertEquals(
                        TipoStatoHackathon.IN_CORSO,
                        stato.aggiorna(
                                DATA_INIZIO,
                                DATA_INIZIO,
                                DATA_FINE
                        ).tipo()
                ),
                () -> assertEquals(
                        TipoStatoHackathon.IN_VALUTAZIONE,
                        stato.aggiorna(
                                DATA_FINE.plusDays(1),
                                DATA_INIZIO,
                                DATA_FINE
                        ).tipo()
                )
        );
    }

    @Test
    void statoInCorsoPassaInValutazioneSoloDopoLaFine() {
        StatoHackathon stato =
                new StatoHackathonInCorso();

        assertAll(
                () -> assertEquals(
                        TipoStatoHackathon.IN_CORSO,
                        stato.aggiorna(
                                DATA_FINE,
                                DATA_INIZIO,
                                DATA_FINE
                        ).tipo()
                ),
                () -> assertEquals(
                        TipoStatoHackathon.IN_VALUTAZIONE,
                        stato.aggiorna(
                                DATA_FINE.plusDays(1),
                                DATA_INIZIO,
                                DATA_FINE
                        ).tipo()
                )
        );
    }

    @Test
    void statoInValutazionePuoConcludereHackathon() {
        StatoHackathon stato =
                new StatoHackathonInValutazione();

        assertEquals(
                TipoStatoHackathon.CONCLUSO,
                stato.concludi().tipo()
        );
    }

    @Test
    void statoConclusoRimaneTerminale() {
        StatoHackathon stato =
                new StatoHackathonConcluso();

        StatoHackathon aggiornato = stato.aggiorna(
                DATA_FINE.plusDays(10),
                DATA_INIZIO,
                DATA_FINE
        );

        assertSame(stato, aggiornato);
    }

    @Test
    void ogniStatoEsponeLeCapabilityCorrette() {
        StatoHackathon inIscrizione =
                new StatoHackathonInIscrizione();
        StatoHackathon inCorso =
                new StatoHackathonInCorso();
        StatoHackathon inValutazione =
                new StatoHackathonInValutazione();
        StatoHackathon concluso =
                new StatoHackathonConcluso();

        assertAll(
                () -> assertTrue(
                        inIscrizione.consenteIscrizioni()
                ),
                () -> assertFalse(
                        inIscrizione.consenteSegnalazioni()
                ),
                () -> assertTrue(
                        inCorso.consenteSegnalazioni()
                ),
                () -> assertFalse(
                        inCorso.consenteValutazioni()
                ),
                () -> assertTrue(
                        inValutazione.consenteSegnalazioni()
                ),
                () -> assertTrue(
                        inValutazione.consenteValutazioni()
                ),
                () -> assertFalse(
                        inValutazione.consenteRiscossionePremio()
                ),
                () -> assertTrue(
                        concluso.consenteRiscossionePremio()
                )
        );
    }
}