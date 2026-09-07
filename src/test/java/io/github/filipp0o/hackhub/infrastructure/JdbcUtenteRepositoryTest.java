package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcUtenteRepositoryTest {

    private EmbeddedDatabase database;
    private JdbcUtenteRepository repository;

    @BeforeEach
    void preparaDatabase() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();

        repository = new JdbcUtenteRepository(JdbcClient.create(database));
    }

    @AfterEach
    void chiudiDatabase() {
        if (database != null) {
            database.shutdown();
        }
    }

    @Test
    void rifiutaClientNullo() {
        assertThrows(
                NullPointerException.class,
                () -> new JdbcUtenteRepository(null)
        );
    }

    @Test
    void recuperaListaVuotaENonTrovaEmailAssente() {
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
        assertFalse(repository.esistePerEmail("uno@example.com"));
    }

    @Test
    void salvaERicostruisceAccountCompleto() {
        Utente nuovo = Utente.crea("uno@example.com", "hash-uno");
        assertNull(nuovo.getId());

        repository.salva(nuovo);

        List<Utente> utenti = repository.recuperaUtentiAssegnabili();

        assertNotNull(nuovo.getId());
        assertTrue(nuovo.getId() > 0);
        assertEquals(1, utenti.size());

        Utente letto = utenti.get(0);
        assertNotSame(nuovo, letto);
        assertEquals(nuovo.getId(), letto.getId());
        assertEquals("uno@example.com", letto.recuperaEmail());
        assertEquals("hash-uno", letto.recuperaPasswordHash());
        assertTrue(repository.esistePerEmail("uno@example.com"));
        assertThrows(UnsupportedOperationException.class, utenti::clear);
    }

    @Test
    void nuovaIstanzaRileggeDatiEUsaIdGeneratiDalDatabase() {
        Utente primo = Utente.crea("uno@example.com", "hash-uno");
        repository.salva(primo);

        JdbcUtenteRepository altraIstanza =
                new JdbcUtenteRepository(JdbcClient.create(database));

        assertTrue(altraIstanza.esistePerEmail("uno@example.com"));

        Utente secondo = Utente.crea("due@example.com", "hash-due");
        altraIstanza.salva(secondo);

        assertNotEquals(primo.getId(), secondo.getId());
        assertEquals(2, repository.recuperaUtentiAssegnabili().size());
    }

    @Test
    void salvareDueVolteLoStessoAccountNonLoDuplica() {
        Utente utente = Utente.crea("uno@example.com", "hash-uno");
        repository.salva(utente);
        Long id = utente.getId();

        repository.salva(utente);

        assertEquals(id, utente.getId());
        assertEquals(1, repository.recuperaUtentiAssegnabili().size());
    }

    @Test
    void aggiornaPerIdAncheConUnOggettoRicostruito() {
        Utente originale = Utente.crea("prima@example.com", "hash-prima");
        repository.salva(originale);

        Utente aggiornato = Utente.ricostruisci(
                originale.getId(), "dopo@example.com", "hash-dopo"
        );

        repository.salva(aggiornato);

        List<Utente> utenti = repository.recuperaUtentiAssegnabili();

        assertEquals(1, utenti.size());
        assertEquals(originale.getId(), utenti.get(0).getId());
        assertEquals("hash-dopo", utenti.get(0).recuperaPasswordHash());
        assertFalse(repository.esistePerEmail("prima@example.com"));
        assertTrue(repository.esistePerEmail("dopo@example.com"));
    }

    @Test
    void rifiutaAggiornamentoDiIdAssente() {
        Utente utente = Utente.ricostruisci(
                99L, "uno@example.com", "hash-uno"
        );

        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(utente)
        );
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }

    @Test
    void vincoloDatabaseRifiutaEmailDuplicataSenzaInserimentiParziali() {
        Utente esistente = Utente.crea(
                "uno@example.com", "hash-originale"
        );
        repository.salva(esistente);

        Utente duplicato = Utente.crea(
                "uno@example.com", "hash-altro"
        );

        IllegalStateException errore = assertThrows(
                IllegalStateException.class,
                () -> repository.salva(duplicato)
        );

        assertEquals("L'email è già registrata", errore.getMessage());
        assertNull(duplicato.getId());

        List<Utente> utenti = repository.recuperaUtentiAssegnabili();
        assertEquals(1, utenti.size());
        assertEquals(
                "hash-originale",
                utenti.get(0).recuperaPasswordHash()
        );
    }

    @Test
    void rifiutaAggiornamentoConEmailDiUnAltroAccount() {
        Utente primo = Utente.crea("uno@example.com", "hash-uno");
        Utente secondo = Utente.crea("due@example.com", "hash-due");
        repository.salva(primo);
        repository.salva(secondo);

        Utente aggiornamento = Utente.ricostruisci(
                secondo.getId(), "uno@example.com", "hash-nuovo"
        );

        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(aggiornamento)
        );

        List<Utente> utenti = repository.recuperaUtentiAssegnabili();
        assertEquals(2, utenti.size());
        assertEquals("due@example.com", utenti.get(1).recuperaEmail());
        assertEquals("hash-due", utenti.get(1).recuperaPasswordHash());
    }

    @Test
    void rifiutaAccountNulloOIncompleto() {
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
    void erroreDiPersistenzaNonAssegnaIdENonLasciaRighe() {
        Utente utente = Utente.crea(
                "a".repeat(321) + "@example.com", "hash-uno"
        );

        IllegalStateException errore = assertThrows(
                IllegalStateException.class,
                () -> repository.salva(utente)
        );

        assertEquals("Impossibile salvare l'utente", errore.getMessage());
        assertNull(utente.getId());
        assertTrue(repository.recuperaUtentiAssegnabili().isEmpty());
    }
}