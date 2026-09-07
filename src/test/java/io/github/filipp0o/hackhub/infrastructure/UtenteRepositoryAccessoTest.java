package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UtenteRepositoryAccessoTest {

    enum TipoRepository {
        IN_MEMORY, JDBC
    }

    private EmbeddedDatabase database;

    @AfterEach
    void chiudiDatabase() {
        if (database != null) {
            database.shutdown();
        }
    }

    @ParameterizedTest
    @EnumSource(TipoRepository.class)
    void recuperaSoloLAccountCorrispondenteConIdECredenziali(
            TipoRepository tipo
    ) {
        UtenteRepository repository = creaRepository(tipo);
        repository.salva(Utente.crea("primo@example.com", "hash-primo"));
        Utente secondo = Utente.crea("secondo@example.com", "hash-secondo");
        repository.salva(secondo);

        Utente recuperato = repository.recuperaPerEmail("secondo@example.com");

        assertNotNull(recuperato);
        assertAll(
                () -> assertEquals(secondo.getId(), recuperato.getId()),
                () -> assertEquals(
                        secondo.recuperaEmail(), recuperato.recuperaEmail()
                ),
                () -> assertEquals(
                        secondo.recuperaPasswordHash(),
                        recuperato.recuperaPasswordHash()
                )
        );
    }

    @ParameterizedTest
    @EnumSource(TipoRepository.class)
    void emailAssenteRestituisceNullSenzaModificareGliAccount(
            TipoRepository tipo
    ) {
        UtenteRepository repository = creaRepository(tipo);
        Utente presente = Utente.crea("presente@example.com", "hash-presente");
        repository.salva(presente);

        assertNull(repository.recuperaPerEmail("assente@example.com"));
        assertEquals(1, repository.recuperaUtentiAssegnabili().size());
        assertNotNull(repository.recuperaPerEmail("presente@example.com"));
    }

    @ParameterizedTest
    @EnumSource(TipoRepository.class)
    void ricercaConfrontaLEmailEsattaComeLaRegistrazione(
            TipoRepository tipo
    ) {
        UtenteRepository repository = creaRepository(tipo);
        repository.salva(Utente.crea("Nome@example.com", "hash"));

        assertAll(
                () -> assertNotNull(repository.recuperaPerEmail("Nome@example.com")),
                () -> assertNull(repository.recuperaPerEmail("nome@example.com")),
                () -> assertNull(repository.recuperaPerEmail(" Nome@example.com "))
        );
    }

    @ParameterizedTest
    @EnumSource(TipoRepository.class)
    void rifiutaEmailNullaOVuota(TipoRepository tipo) {
        UtenteRepository repository = creaRepository(tipo);

        for (String email : Arrays.asList(null, "", "   ", "\t\n")) {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> repository.recuperaPerEmail(email)
            );
        }
    }

    @ParameterizedTest
    @EnumSource(TipoRepository.class)
    void ricercaRifletteGliAggiornamentiDiEmailEHash(TipoRepository tipo) {
        UtenteRepository repository = creaRepository(tipo);
        Utente originale = Utente.crea("prima@example.com", "hash-prima");
        repository.salva(originale);
        Utente aggiornato = Utente.ricostruisci(
                originale.getId(), "dopo@example.com", "hash-dopo"
        );
        repository.salva(aggiornato);

        assertNull(repository.recuperaPerEmail("prima@example.com"));
        Utente recuperato = repository.recuperaPerEmail("dopo@example.com");
        assertNotNull(recuperato);
        assertEquals(originale.getId(), recuperato.getId());
        assertEquals("hash-dopo", recuperato.recuperaPasswordHash());
    }

    @ParameterizedTest
    @EnumSource(TipoRepository.class)
    void trattaLEmailComeDatoAncheQuandoContieneApici(TipoRepository tipo) {
        UtenteRepository repository = creaRepository(tipo);
        String email = "o'connor@example.com";
        repository.salva(Utente.crea(email, "hash"));

        assertNotNull(repository.recuperaPerEmail(email));
        assertNull(repository.recuperaPerEmail("' OR '1'='1' --"));
        assertEquals(1, repository.recuperaUtentiAssegnabili().size());
    }

    @Test
    void ignoraIRiferimentiConSoloIdNellaRicercaInMemoria() {
        InMemoryUtenteRepository repository =
                new InMemoryUtenteRepository(List.of(new Utente(1L)));
        Utente account = Utente.crea("account@example.com", "hash");
        repository.salva(account);

        assertSame(account, repository.recuperaPerEmail("account@example.com"));
        assertNull(repository.recuperaPerEmail("assente@example.com"));
    }

    @Test
    void erroreJdbcNonVieneConfusoConUnUtenteAssente() {
        UtenteRepository repository = creaRepository(TipoRepository.JDBC);
        JdbcClient.create(database).sql("DROP ALL OBJECTS").update();

        IllegalStateException errore = assertThrows(
                IllegalStateException.class,
                () -> repository.recuperaPerEmail("account@example.com")
        );
        assertInstanceOf(DataAccessException.class, errore.getCause());
    }

    private UtenteRepository creaRepository(TipoRepository tipo) {
        if (tipo == TipoRepository.IN_MEMORY) {
            return new InMemoryUtenteRepository(List.of());
        }

        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();
        return new JdbcUtenteRepository(JdbcClient.create(database));
    }
}
