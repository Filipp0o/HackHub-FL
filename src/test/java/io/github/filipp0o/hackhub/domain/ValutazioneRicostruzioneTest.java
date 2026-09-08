package io.github.filipp0o.hackhub.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ValutazioneRicostruzioneTest {

    private final Utente giudice = new Utente(2L);

    private final DatiValutazione dati = new DatiValutazione(
            "Ottimo progetto", new BigDecimal("8.5")
    );

    private final LocalDateTime dataOriginale =
            LocalDateTime.of(2025, 4, 10, 15, 30, 12, 123456000);

    @Test
    void ricostruisceIdDatiTimestampECollegamentoBidirezionale() {
        Sottomissione sottomissione = creaSottomissione();

        Valutazione valutazione = Valutazione.ricostruisci(
                42L, sottomissione, giudice, dati, dataOriginale
        );

        assertEquals(42L, valutazione.getId());
        assertEquals(dati.giudizio(), valutazione.getGiudizio());
        assertEquals(dati.punteggio(), valutazione.getPunteggio());
        assertEquals(dataOriginale, valutazione.getDataOra());
        assertSame(giudice, valutazione.getGiudice());
        assertSame(sottomissione, valutazione.getSottomissione());
        assertSame(valutazione, sottomissione.getValutazione());
    }

    @Test
    void idInvalidoNonCollegaOggettiParziali() {
        for (Long id : new Long[]{null, 0L, -1L}) {
            Sottomissione sottomissione = creaSottomissione();

            Class<? extends RuntimeException> tipo = id == null
                    ? NullPointerException.class
                    : IllegalArgumentException.class;

            assertThrows(
                    tipo,
                    () -> Valutazione.ricostruisci(
                            id, sottomissione, giudice, dati, dataOriginale
                    )
            );

            assertNull(sottomissione.getValutazione());
        }
    }

    @Test
    void dataOraAssenteNonCollegaValutazione() {
        Sottomissione sottomissione = creaSottomissione();

        assertThrows(
                NullPointerException.class,
                () -> Valutazione.ricostruisci(
                        42L, sottomissione, giudice, dati, null
                )
        );

        assertNull(sottomissione.getValutazione());
    }

    @Test
    void datiInvalidiMantengonoIControlliDelDominio() {
        for (DatiValutazione nonValidi : new DatiValutazione[]{
                new DatiValutazione(" ", BigDecimal.ONE),
                new DatiValutazione("Giudizio", new BigDecimal("-1")),
                new DatiValutazione("Giudizio", new BigDecimal("11"))
        }) {
            Sottomissione sottomissione = creaSottomissione();

            assertThrows(
                    IllegalArgumentException.class,
                    () -> Valutazione.ricostruisci(
                            42L, sottomissione, giudice,
                            nonValidi, dataOriginale
                    )
            );

            assertNull(sottomissione.getValutazione());
        }
    }

    @Test
    void secondaRicostruzioneNonSovrascriveValutazioneEsistente() {
        Sottomissione sottomissione = creaSottomissione();

        Valutazione prima = Valutazione.ricostruisci(
                42L, sottomissione, giudice, dati, dataOriginale
        );

        assertThrows(
                IllegalStateException.class,
                () -> Valutazione.ricostruisci(
                        43L, sottomissione, giudice,
                        dati, dataOriginale.plusDays(1)
                )
        );

        assertSame(prima, sottomissione.getValutazione());
        assertEquals(dataOriginale, prima.getDataOra());
    }

    @Test
    void creazioneRestaSenzaIdEAssegnazioneAvvieneUnaSolaVolta() {
        Valutazione nuova = Valutazione.crea(
                creaSottomissione(), giudice, dati
        );

        assertNull(nuova.getId());
        assertNotNull(nuova.getDataOra());

        assertThrows(
                NullPointerException.class,
                () -> nuova.assegnaId(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> nuova.assegnaId(0L)
        );
        assertNull(nuova.getId());

        nuova.assegnaId(7L);

        assertThrows(
                IllegalStateException.class,
                () -> nuova.assegnaId(8L)
        );
        assertEquals(7L, nuova.getId());
    }

    private Sottomissione creaSottomissione() {
        Utente responsabile = new Utente(4L);
        Team team = Team.crea("Team", responsabile, responsabile);
        LocalDate data = LocalDate.of(2025, 4, 1);

        Hackathon hackathon = Hackathon.crea(
                new DatiHackathon(
                        "Hackathon",
                        "Regolamento",
                        "Criteri",
                        data,
                        data.plusDays(1),
                        data.plusDays(2),
                        "Camerino",
                        BigDecimal.TEN,
                        5
                ),
                new Utente(1L),
                giudice,
                List.of(new Utente(3L))
        );

        return Sottomissione.crea(
                Partecipazione.crea(hackathon, team),
                "Progetto"
        );
    }
}