package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtenteRepositoryImplTest {

    @Test
    void rifiutaListaUtentiNulla() {
        assertThrows(
                NullPointerException.class,
                () -> new InMemoryUtenteRepository(null)
        );
    }

    @Test
    void rifiutaUtentiNulliNellaLista() {
        List<Utente> utenti = new ArrayList<>();
        utenti.add(new Utente(1L));
        utenti.add(null);

        assertThrows(
                NullPointerException.class,
                () -> new InMemoryUtenteRepository(utenti)
        );
    }

    @Test
    void recuperaGliUtentiAssegnabili() {
        Utente primo = new Utente(1L);
        Utente secondo = new Utente(2L);

        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of(primo, secondo));

        assertEquals(
                List.of(primo, secondo),
                repository.recuperaUtentiAssegnabili()
        );
    }

    @Test
    void conservaUnaCopiaDellaListaRicevuta() {
        Utente primo = new Utente(1L);
        List<Utente> originali = new ArrayList<>();
        originali.add(primo);

        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(originali);

        originali.add(new Utente(2L));

        assertEquals(
                List.of(primo),
                repository.recuperaUtentiAssegnabili()
        );
    }

    @Test
    void restituisceUnaListaNonModificabile() {
        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(
                        List.of(new Utente(1L))
                );

        List<Utente> risultato =
                repository.recuperaUtentiAssegnabili();

        assertThrows(
                UnsupportedOperationException.class,
                () -> risultato.add(new Utente(2L))
        );
    }

    @Test
    void salvaUnNuovoAccountEAssegnaId() {
        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of());

        Utente utente = Utente.crea(
                "uno@example.com",
                "hash-uno"
        );

        assertNull(utente.getId());
        assertFalse(repository.esistePerEmail("uno@example.com"));

        repository.salva(utente);

        assertEquals(Long.valueOf(1L), utente.getId());
        assertTrue(repository.esistePerEmail("uno@example.com"));
        assertFalse(repository.esistePerEmail("assente@example.com"));
        assertEquals(
                List.of(utente),
                repository.recuperaUtentiAssegnabili()
        );
    }

    @Test
    void generaIdSuccessiviAlMassimoIniziale() {
        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(
                        List.of(new Utente(42L), new Utente(3L))
                );

        Utente primo = Utente.crea(
                "uno@example.com",
                "hash-uno"
        );
        Utente secondo = Utente.crea(
                "due@example.com",
                "hash-due"
        );

        repository.salva(primo);
        repository.salva(secondo);

        assertEquals(Long.valueOf(43L), primo.getId());
        assertEquals(Long.valueOf(44L), secondo.getId());
        assertEquals(
                4,
                repository.recuperaUtentiAssegnabili().size()
        );
    }

    @Test
    void aggiornaPerIdSenzaDuplicareUtenti() {
        Utente originale = Utente.ricostruisci(
                7L,
                "prima@example.com",
                "hash-prima"
        );

        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of(originale));

        Utente aggiornato = Utente.ricostruisci(
                7L,
                "dopo@example.com",
                "hash-dopo"
        );

        repository.salva(aggiornato);
        repository.salva(aggiornato);

        assertEquals(
                List.of(aggiornato),
                repository.recuperaUtentiAssegnabili()
        );
        assertFalse(repository.esistePerEmail("prima@example.com"));
        assertTrue(repository.esistePerEmail("dopo@example.com"));
    }

    @Test
    void rifiutaEmailDuplicataSenzaAssegnareId() {
        Utente esistente = Utente.ricostruisci(
                1L,
                "uno@example.com",
                "hash-uno"
        );

        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of(esistente));

        Utente duplicato = Utente.crea(
                "uno@example.com",
                "hash-altro"
        );

        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(duplicato)
        );

        assertNull(duplicato.getId());
        assertEquals(
                List.of(esistente),
                repository.recuperaUtentiAssegnabili()
        );

        Utente nuovo = Utente.crea(
                "due@example.com",
                "hash-due"
        );
        repository.salva(nuovo);

        assertEquals(Long.valueOf(2L), nuovo.getId());
    }

    @Test
    void rifiutaAggiornamentoConEmailDiUnAltroUtente() {
        Utente primo = Utente.ricostruisci(
                1L,
                "uno@example.com",
                "hash-uno"
        );
        Utente secondo = Utente.ricostruisci(
                2L,
                "due@example.com",
                "hash-due"
        );

        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of(primo, secondo));

        Utente aggiornamento = Utente.ricostruisci(
                2L,
                "uno@example.com",
                "hash-nuovo"
        );

        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(aggiornamento)
        );

        assertEquals(
                List.of(primo, secondo),
                repository.recuperaUtentiAssegnabili()
        );
    }

    @Test
    void rifiutaAggiornamentoDiUnIdAssente() {
        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of());

        Utente utente = Utente.ricostruisci(
                9L,
                "uno@example.com",
                "hash-uno"
        );

        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(utente)
        );

        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
        assertFalse(repository.esistePerEmail("uno@example.com"));
    }

    @Test
    void rifiutaSalvataggioNulloOAccountIncompleto() {
        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of());

        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.salva(new Utente(1L))
        );

        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void rifiutaRicercaSenzaEmail() {
        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of());

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.esistePerEmail(null)
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> repository.esistePerEmail(" ")
        );
    }

    @Test
    void rifiutaDuplicatiNeiDatiIniziali() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new InMemoryUtenteRepository(
                        List.of(new Utente(1L), new Utente(1L))
                )
        );

        assertThrows(
                IllegalStateException.class,
                () -> new InMemoryUtenteRepository(
                        List.of(
                                Utente.ricostruisci(
                                        1L,
                                        "uno@example.com",
                                        "hash-uno"
                                ),
                                Utente.ricostruisci(
                                        2L,
                                        "uno@example.com",
                                        "hash-due"
                                )
                        )
                )
        );
    }

    @Test
    void unaListaGiaRestituitaNonCambiaDopoIlSalvataggio() {
        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of());

        List<Utente> prima =
                repository.recuperaUtentiAssegnabili();

        repository.salva(
                Utente.crea("uno@example.com", "hash-uno")
        );

        assertTrue(prima.isEmpty());
        assertEquals(
                1,
                repository.recuperaUtentiAssegnabili().size()
        );
    }
}