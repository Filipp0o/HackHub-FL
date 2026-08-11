package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValutazioneTest {

    private final Utente giudice = new Utente(1L);

    @Test
    void creaValutazioneValidaECollegaLaSottomissione() {
        Sottomissione sottomissione =
                nuovaSottomissione();

        DatiValutazione dati = new DatiValutazione(
                "Progetto completo e ben realizzato",
                BigDecimal.valueOf(8)
        );

        Valutazione valutazione = Valutazione.crea(
                sottomissione,
                giudice,
                dati
        );

        assertAll(
                () -> assertEquals(
                        dati.giudizio(),
                        valutazione.getGiudizio()
                ),
                () -> assertEquals(
                        dati.punteggio(),
                        valutazione.getPunteggio()
                ),
                () -> assertNotNull(
                        valutazione.getDataOra()
                ),
                () -> assertSame(
                        sottomissione,
                        valutazione.getSottomissione()
                ),
                () -> assertSame(
                        giudice,
                        valutazione.getGiudice()
                ),
                () -> assertSame(
                        valutazione,
                        sottomissione.getValutazione()
                )
        );
    }

    @Test
    void rifiutaSottomissioneNulla() {
        assertThrows(
                NullPointerException.class,
                () -> Valutazione.crea(
                        null,
                        giudice,
                        datiValidi()
                )
        );
    }

    @Test
    void rifiutaGiudiceNullo() {
        assertThrows(
                NullPointerException.class,
                () -> Valutazione.crea(
                        nuovaSottomissione(),
                        null,
                        datiValidi()
                )
        );
    }

    @Test
    void rifiutaDatiValutazioneNulli() {
        assertThrows(
                NullPointerException.class,
                () -> Valutazione.crea(
                        nuovaSottomissione(),
                        giudice,
                        null
                )
        );
    }

    @Test
    void rifiutaGiudizioNulloOVuoto() {
        for (String giudizio : new String[]{null, "   "}) {
            DatiValutazione dati = new DatiValutazione(
                    giudizio,
                    BigDecimal.valueOf(8)
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> Valutazione.crea(
                            nuovaSottomissione(),
                            giudice,
                            dati
                    )
            );
        }
    }

    @Test
    void rifiutaPunteggioNullo() {
        DatiValutazione dati = new DatiValutazione(
                "Giudizio valido",
                null
        );

        assertThrows(
                NullPointerException.class,
                () -> Valutazione.crea(
                        nuovaSottomissione(),
                        giudice,
                        dati
                )
        );
    }

    @Test
    void accettaPunteggiAgliEstremi() {
        Valutazione valutazioneMinima = Valutazione.crea(
                nuovaSottomissione(),
                giudice,
                new DatiValutazione(
                        "Valutazione minima",
                        BigDecimal.ZERO
                )
        );

        Valutazione valutazioneMassima = Valutazione.crea(
                nuovaSottomissione(),
                giudice,
                new DatiValutazione(
                        "Valutazione massima",
                        BigDecimal.TEN
                )
        );

        assertAll(
                () -> assertEquals(
                        BigDecimal.ZERO,
                        valutazioneMinima.getPunteggio()
                ),
                () -> assertEquals(
                        BigDecimal.TEN,
                        valutazioneMassima.getPunteggio()
                )
        );
    }

    @Test
    void rifiutaPunteggioFuoriIntervallo() {
        for (BigDecimal punteggio : new BigDecimal[]{
                BigDecimal.valueOf(-1),
                BigDecimal.valueOf(11)
        }) {
            DatiValutazione dati = new DatiValutazione(
                    "Giudizio valido",
                    punteggio
            );

            assertThrows(
                    IllegalArgumentException.class,
                    () -> Valutazione.crea(
                            nuovaSottomissione(),
                            giudice,
                            dati
                    )
            );
        }
    }

    @Test
    void impedisceSecondaValutazioneDellaStessaSottomissione() {
        Sottomissione sottomissione =
                nuovaSottomissione();

        Valutazione primaValutazione = Valutazione.crea(
                sottomissione,
                giudice,
                datiValidi()
        );

        assertThrows(
                IllegalStateException.class,
                () -> Valutazione.crea(
                        sottomissione,
                        giudice,
                        new DatiValutazione(
                                "Seconda valutazione",
                                BigDecimal.valueOf(9)
                        )
                )
        );

        assertSame(
                primaValutazione,
                sottomissione.getValutazione()
        );
    }

    private Sottomissione nuovaSottomissione() {
        return new Sottomissione(
                creaPartecipazioneValida(),
                "Repository del progetto"
        );
    }

    private Partecipazione creaPartecipazioneValida() {
        Hackathon hackathon = creaHackathonValido();
        Utente responsabile = new Utente(5L);

        Team team = Team.crea(
                "Team Alpha",
                responsabile,
                responsabile
        );

        return new Partecipazione(hackathon, team);
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
                giudice,
                List.of(new Utente(3L))
        );
    }

    private DatiValutazione datiValidi() {
        return new DatiValutazione(
                "Progetto completo e ben realizzato",
                BigDecimal.valueOf(8)
        );
    }
}