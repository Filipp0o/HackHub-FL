package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcTeamRepositoryTest {

    private EmbeddedDatabase database;
    private JdbcClient jdbc;
    private JdbcTeamRepository repository;
    private Utente primo;
    private Utente secondo;

    @BeforeEach
    void prepara() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();

        jdbc = JdbcClient.create(database);
        repository = new JdbcTeamRepository(database);

        var utenti = new JdbcUtenteRepository(jdbc);
        primo = Utente.crea("uno@example.com", "hash-uno");
        secondo = Utente.crea("due@example.com", "hash-due");
        utenti.salva(primo);
        utenti.salva(secondo);
    }

    @AfterEach
    void chiudi() {
        if (database != null) {
            database.shutdown();
        }
    }

    private Team crea() {
        Team team = Team.crea("ByteBuilders", primo, primo);
        repository.salva(team);
        return team;
    }

    @Test
    void salvaERicostruisceTeamConAccountCompleti() {
        Team team = crea();
        team.aggiungiMembro(secondo);
        repository.salva(team);

        Team letto = new JdbcTeamRepository(database)
                .recuperaTeam(secondo);

        assertNotNull(team.getId());
        assertNotSame(team, letto);
        assertEquals(team.getId(), letto.getId());
        assertEquals("ByteBuilders", letto.getNome());
        assertEquals(
                List.of(primo.getId(), secondo.getId()),
                letto.getMembri().stream().map(Utente::getId).toList()
        );
        assertEquals(
                "uno@example.com",
                letto.getResponsabile().recuperaEmail()
        );
        assertEquals(
                "hash-due",
                letto.getMembri().get(1).recuperaPasswordHash()
        );

        assertTrue(repository.verificaAppartenenzaTeam(secondo));
        assertEquals(
                team.getId(),
                repository.recuperaTeamCreatoDa(primo).getId()
        );
        assertThrows(
                TeamRepository.TeamNonCreatoException.class,
                () -> repository.recuperaTeamCreatoDa(secondo)
        );
    }

    @Test
    void salvataggiRipetutiNonDuplicanoRigheEIdSonoGeneratiDalDatabase() {
        Team team = crea();
        repository.salva(team);

        Team altro = Team.crea("Altro", secondo, secondo);
        new JdbcTeamRepository(database).salva(altro);

        assertNotEquals(team.getId(), altro.getId());
        assertEquals(
                2L,
                jdbc.sql("SELECT COUNT(*) FROM team")
                        .query(Long.class).single()
        );
        assertEquals(
                2L,
                jdbc.sql("SELECT COUNT(*) FROM team_membro")
                        .query(Long.class).single()
        );
    }

    @Test
    void aggiornaNomeResponsabileEMembriDaOggettoRicostruito() {
        Team team = crea();

        repository.salva(Team.ricostruisci(
                team.getId(), "Nuovo", List.of(secondo), secondo
        ));

        Team letto = repository.recuperaTeam(secondo);

        assertEquals(team.getId(), letto.getId());
        assertEquals("Nuovo", letto.getNome());
        assertEquals(secondo.getId(), letto.getResponsabile().getId());
        assertFalse(repository.verificaAppartenenzaTeam(primo));
    }

    @Test
    void utenteSenzaTeamEIdAssenteSonoGestiti() {
        assertFalse(repository.verificaAppartenenzaTeam(primo));
        assertFalse(repository.verificaAppartenenzaTeam(
                Utente.crea("nuovo@example.com", "hash")
        ));

        assertThrows(
                IllegalStateException.class,
                () -> repository.recuperaTeam(primo)
        );
        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(Team.ricostruisci(
                        999L, "Assente", List.of(primo), primo
                ))
        );
        assertEquals(
                0L,
                jdbc.sql("SELECT COUNT(*) FROM team")
                        .query(Long.class).single()
        );
    }

    @Test
    void nuovoTeamConMembroGiaOccupatoVieneAnnullatoSenzaAssegnareId() {
        Team esistente = crea();
        Team nuovo = Team.crea("Altro", secondo, secondo);
        nuovo.aggiungiMembro(primo);

        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(nuovo)
        );

        assertNull(nuovo.getId());
        assertFalse(repository.verificaAppartenenzaTeam(secondo));
        assertEquals(
                esistente.getId(),
                repository.recuperaTeam(primo).getId()
        );
        assertEquals(
                1L,
                jdbc.sql("SELECT COUNT(*) FROM team")
                        .query(Long.class).single()
        );
        assertEquals(
                1L,
                jdbc.sql("SELECT COUNT(*) FROM team_membro")
                        .query(Long.class).single()
        );
    }

    @Test
    void erroreAggiornamentoRipristinaNomeEMembriPrecedenti() {
        Team team = crea();
        repository.salva(Team.crea("Altro", secondo, secondo));

        Team aggiornato = Team.ricostruisci(
                team.getId(), "Modificato",
                List.of(primo, secondo), primo
        );

        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(aggiornato)
        );

        Team letto = repository.recuperaTeam(primo);

        assertEquals("ByteBuilders", letto.getNome());
        assertEquals(1, letto.numeroMembri());
        assertNotEquals(
                team.getId(),
                repository.recuperaTeam(secondo).getId()
        );
    }

    @Test
    void membroInesistenteAnnullaInserimentoDelTeam() {
        Team team = Team.crea("Nuovo", primo, primo);
        team.aggiungiMembro(new Utente(999L));

        assertThrows(
                IllegalStateException.class,
                () -> repository.salva(team)
        );

        assertNull(team.getId());
        assertEquals(
                0L,
                jdbc.sql("SELECT COUNT(*) FROM team")
                        .query(Long.class).single()
        );
        assertEquals(
                0L,
                jdbc.sql("SELECT COUNT(*) FROM team_membro")
                        .query(Long.class).single()
        );
    }

    @Test
    void partecipaAllaTransazioneEsternaSulMedesimoDatasource() {
        Team team = crea();
        var esterna = new TransactionTemplate(
                new JdbcTransactionManager(database)
        );

        assertThrows(
                IllegalStateException.class,
                () -> esterna.executeWithoutResult(stato -> {
                    Team letto = repository.recuperaTeam(primo);
                    letto.aggiungiMembro(secondo);
                    repository.salva(letto);

                    throw new IllegalStateException(
                            "Errore successivo al salvataggio"
                    );
                })
        );

        assertEquals(1, repository.recuperaTeam(primo).numeroMembri());
        assertFalse(repository.verificaAppartenenzaTeam(secondo));
        assertEquals(team.getId(), repository.recuperaTeam(primo).getId());
    }

    @Test
    void rifiutaDipendenzeNulleEAccountNonSalvati() {
        assertThrows(
                NullPointerException.class,
                () -> new JdbcTeamRepository(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> repository.salva(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> repository.recuperaTeam(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> repository.verificaAppartenenzaTeam(null)
        );

        Utente nuovo = Utente.crea("nuovo@example.com", "hash");

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.salva(Team.crea("Nuovo", nuovo, nuovo))
        );
    }
}