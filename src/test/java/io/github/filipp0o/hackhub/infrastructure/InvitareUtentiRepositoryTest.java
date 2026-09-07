package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class InvitareUtentiRepositoryTest {

    private EmbeddedDatabase database;

    @AfterEach
    void chiudiDatabase() {
        if (database != null) {
            database.shutdown();
        }
    }

    private UtenteRepository repositoryUtenti(boolean jdbc) {
        if (!jdbc) {
            return new InMemoryUtenteRepository(List.of());
        }
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();
        return new JdbcUtenteRepository(JdbcClient.create(database));
    }

    private Utente registra(UtenteRepository repository, String nome) {
        Utente utente = Utente.crea(nome + "@example.com", "hash-" + nome);
        repository.salva(utente);
        return utente;
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void recuperaAltriAccountEscludendoRichiedentePerId(boolean jdbc) {
        UtenteRepository repository = repositoryUtenti(jdbc);
        Utente richiedente = registra(repository, "richiedente");
        Utente primo = registra(repository, "primo");
        Utente secondo = registra(repository, "secondo");

        var invitabili = repository.recuperaUtentiInvitabili(new Utente(richiedente.getId()));

        assertEquals(List.of(primo.getId(), secondo.getId()),
                invitabili.stream().map(Utente::getId).toList());
        assertEquals(3, repository.recuperaUtentiAssegnabili().size());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void unicoAccountNonHaAltriUtentiInvitabili(boolean jdbc) {
        UtenteRepository repository = repositoryUtenti(jdbc);
        Utente richiedente = registra(repository, "richiedente");

        assertTrue(repository.recuperaUtentiInvitabili(richiedente).isEmpty());
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void rifiutaRichiedenteNulloOSenzaId(boolean jdbc) {
        UtenteRepository repository = repositoryUtenti(jdbc);

        assertThrows(NullPointerException.class,
                () -> repository.recuperaUtentiInvitabili(null));
        assertThrows(IllegalArgumentException.class,
                () -> repository.recuperaUtentiInvitabili(Utente.crea("a@example.com", "hash")));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void listaInvitabiliEUnoSnapshotNonModificabile(boolean jdbc) {
        UtenteRepository repository = repositoryUtenti(jdbc);
        Utente richiedente = registra(repository, "richiedente");
        registra(repository, "primo");
        var snapshot = repository.recuperaUtentiInvitabili(richiedente);

        assertThrows(UnsupportedOperationException.class, snapshot::clear);
        registra(repository, "secondo");
        assertEquals(1, snapshot.size());
        assertEquals(2, repository.recuperaUtentiInvitabili(richiedente).size());
    }

    @Test
    void riferimentiLegacySenzaCredenzialiNonSonoAccountInvitabili() {
        Utente richiedente = Utente.ricostruisci(1L, "a@example.com", "hash-a");
        Utente candidato = Utente.ricostruisci(2L, "b@example.com", "hash-b");
        UtenteRepository repository = new InMemoryUtenteRepository(
                List.of(richiedente, candidato, new Utente(3L))
        );

        assertEquals(List.of(candidato), repository.recuperaUtentiInvitabili(richiedente));
    }

    @Test
    void recuperaTeamDelCreatoreAncheConUnaDiversaIstanzaUtente() {
        TeamRepository repository = new InMemoryTeamRepository();
        Utente creatore = new Utente(1L);
        Utente altro = new Utente(2L);
        Team team = Team.crea("Primo", creatore, creatore);
        repository.salva(Team.crea("Secondo", altro, altro));
        repository.salva(team);

        assertSame(team, repository.recuperaTeamCreatoDa(new Utente(1L)));
        assertSame(team, repository.recuperaTeamCreatoDa(creatore));
    }

    @Test
    void sempliceMembroNonPuoRecuperareTeamComeCreatore() {
        TeamRepository repository = new InMemoryTeamRepository();
        Utente creatore = new Utente(1L);
        Utente membro = new Utente(2L);
        Team team = Team.crea("Primo", creatore, creatore);
        team.aggiungiMembro(membro);
        repository.salva(team);

        assertSame(team, repository.recuperaTeam(membro));
        assertThrows(IllegalStateException.class,
                () -> repository.recuperaTeamCreatoDa(membro));
        assertEquals(2, team.numeroMembri());
    }

    @Test
    void rifiutaUtenteSenzaTeamONullo() {
        TeamRepository repository = new InMemoryTeamRepository();

        assertThrows(IllegalStateException.class,
                () -> repository.recuperaTeamCreatoDa(new Utente(1L)));
        assertThrows(NullPointerException.class,
                () -> repository.recuperaTeamCreatoDa(null));
    }
}
