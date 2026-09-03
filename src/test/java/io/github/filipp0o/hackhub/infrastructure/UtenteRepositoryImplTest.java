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
        Utente primoUtente = new Utente(1L);
        Utente secondoUtente = new Utente(2L);

        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(
                        List.of(
                                primoUtente,
                                secondoUtente
                        )
                );

        List<Utente> risultato =
                repository.recuperaUtentiAssegnabili();

        assertEquals(
                List.of(primoUtente, secondoUtente),
                risultato
        );
    }

    @Test
    void conservaUnaCopiaDellaListaRicevuta() {
        Utente primoUtente = new Utente(1L);
        Utente secondoUtente = new Utente(2L);

        List<Utente> utentiOriginali =
                new ArrayList<>();

        utentiOriginali.add(primoUtente);

        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(
                        utentiOriginali
                );

        utentiOriginali.add(secondoUtente);

        assertEquals(
                List.of(primoUtente),
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
}