package io.github.filipp0o.hackhub.domain;

import io.github.filipp0o.hackhub.infrastructure.SottomissioneRepositoryImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SottomissioneRicostruzioneTest {

    @Test
    void ricostruisceIdentitaContenutoEAssociazioneConValutazione() {
        Partecipazione partecipazione = creaPartecipazione();

        Sottomissione s = Sottomissione.ricostruisci(
                42L, partecipazione, "Progetto"
        );

        assertEquals(42L, s.getId());
        assertEquals("Progetto", s.getContenuto());
        assertSame(partecipazione, s.getPartecipazione());
        assertSame(s, partecipazione.getSottomissione());
        assertNull(s.getValutazione());

        LocalDateTime data = LocalDateTime.of(2025, 4, 10, 12, 0);

        Valutazione v = Valutazione.ricostruisci(
                7L,
                s,
                new Utente(2L),
                new DatiValutazione("Buono", BigDecimal.TEN),
                data
        );

        assertSame(v, s.getValutazione());
        assertSame(s, v.getSottomissione());
        assertEquals(data, v.getDataOra());
    }

    @Test
    void idOContenutoInvalidiNonLascianoAssociazioniParziali() {
        Partecipazione p = creaPartecipazione();

        assertThrows(
                NullPointerException.class,
                () -> Sottomissione.ricostruisci(null, p, "Progetto")
        );

        for (long id : new long[]{0L, -1L}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Sottomissione.ricostruisci(id, p, "Progetto")
            );
        }

        for (String contenuto : new String[]{null, "", " "}) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> Sottomissione.ricostruisci(1L, p, contenuto)
            );
        }

        assertNull(p.getSottomissione());
    }

    @Test
    void secondaRicostruzioneNonSostituisceSottomissione() {
        Partecipazione p = creaPartecipazione();

        Sottomissione prima = Sottomissione.ricostruisci(
                1L, p, "Prima"
        );

        assertThrows(
                IllegalStateException.class,
                () -> Sottomissione.ricostruisci(2L, p, "Seconda")
        );

        assertSame(prima, p.getSottomissione());
        assertEquals("Prima", prima.getContenuto());
    }

    @Test
    void salvataggioAssegnaIdDistintiEConservaAggiornamenti() {
        var repository = new SottomissioneRepositoryImpl();

        Sottomissione prima = Sottomissione.crea(
                creaPartecipazione(), "Prima"
        );
        Sottomissione seconda = Sottomissione.crea(
                creaPartecipazione(), "Seconda"
        );

        assertNull(prima.getId());
        assertNull(seconda.getId());

        repository.salva(prima);
        repository.salva(seconda);

        assertTrue(prima.getId() > 0);
        assertNotEquals(prima.getId(), seconda.getId());

        Long id = prima.getId();
        prima.aggiornaContenuto("Aggiornata");
        repository.salva(prima);

        assertEquals(id, prima.getId());
        assertEquals(
                "Aggiornata",
                repository.recuperaSottomissione(
                        prima.getPartecipazione()
                ).getContenuto()
        );
        assertSame(
                seconda,
                repository.recuperaSottomissione(
                        seconda.getPartecipazione()
                )
        );
    }

    @Test
    void idPersistitoRiallineaContatoreDelRepository() {
        var repository = new SottomissioneRepositoryImpl();

        Sottomissione letta = Sottomissione.ricostruisci(
                50L, creaPartecipazione(), "Letta"
        );
        repository.salva(letta);

        Sottomissione nuova = Sottomissione.crea(
                creaPartecipazione(), "Nuova"
        );
        repository.salva(nuova);

        assertTrue(nuova.getId() > 50L);
        assertSame(
                letta,
                repository.recuperaSottomissione(
                        letta.getPartecipazione()
                )
        );
    }

    @Test
    void assegnazioneIdRifiutaValoriInvalidiERiassegnazioni() {
        Sottomissione s = Sottomissione.crea(
                creaPartecipazione(), "Progetto"
        );

        assertThrows(
                NullPointerException.class,
                () -> s.assegnaId(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> s.assegnaId(0L)
        );
        assertNull(s.getId());

        s.assegnaId(7L);

        assertThrows(
                IllegalStateException.class,
                () -> s.assegnaId(8L)
        );
        assertEquals(7L, s.getId());
    }

    private Partecipazione creaPartecipazione() {
        Utente responsabile = new Utente(4L);
        LocalDate data = LocalDate.of(2025, 4, 1);

        Hackathon h = Hackathon.crea(
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
                new Utente(2L),
                List.of(new Utente(3L))
        );

        return Partecipazione.crea(
                h, Team.crea("Team", responsabile, responsabile)
        );
    }
}