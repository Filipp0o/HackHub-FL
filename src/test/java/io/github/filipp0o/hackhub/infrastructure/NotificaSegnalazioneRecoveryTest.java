package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NotificaSegnalazioneRecoveryTest {

    private EmbeddedDatabase database;
    private Utente organizzatore;
    private NotificaSegnalazione notifica;
    private SegnalazioneRepository repository;
    private EsaminareSegnalazioneControl control;

    @AfterEach
    void chiudi() {
        if (database != null) {
            database.shutdown();
        }
    }

    private void prepara(boolean jdbcAttivo, boolean letta) {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();

        var utenti = new JdbcUtenteRepository(
                JdbcClient.create(database)
        );

        organizzatore = Utente.crea(
                "organizzatore@example.com",
                "hash-organizzatore"
        );

        Utente mentore = Utente.crea(
                "mentore@example.com",
                "hash-mentore"
        );

        Utente giudice = Utente.crea(
                "giudice@example.com",
                "hash-giudice"
        );

        Utente membro = Utente.crea(
                "membro@example.com",
                "hash-membro"
        );

        List.of(
                organizzatore,
                mentore,
                giudice,
                membro
        ).forEach(utenti::salva);

        Team team = Team.crea("Team Alpha", membro, membro);
        new JdbcTeamRepository(database).salva(team);

        LocalDate oggi = LocalDate.now();
        var hackathons = new JdbcHackathonRepository(database);

        Hackathon hackathon = Hackathon.crea(
                new DatiHackathon(
                        "HackHub",
                        "Regolamento",
                        "Criteri",
                        oggi.minusDays(4),
                        oggi.minusDays(2),
                        oggi.plusDays(2),
                        "Camerino",
                        BigDecimal.TEN,
                        5
                ),
                organizzatore,
                giudice,
                List.of(mentore)
        );

        hackathons.salva(hackathon);
        hackathon.aggiornaStato(oggi);

        var partecipazioni = new JdbcPartecipazioneRepository(
                database,
                hackathons
        );

        Partecipazione partecipazione = Partecipazione.crea(
                hackathon,
                team
        );

        partecipazioni.salva(partecipazione);

        Segnalazione segnalazione = Segnalazione.crea(
                mentore,
                partecipazione,
                "Violazione"
        );

        notifica = NotificaSegnalazione.crea(
                segnalazione,
                organizzatore
        );

        if (letta) {
            notifica.segnaComeLetta();
        }

        repository = spy(jdbcAttivo
                ? new JdbcSegnalazioneRepository(
                database,
                hackathons,
                partecipazioni
        )
                : new SegnalazioneRepositoryImpl());

        repository.salvaConNotifica(segnalazione, notifica);

        control = new EsaminareSegnalazioneControl(
                repository,
                partecipazioni
        );
    }

    private void apri() {
        assertSame(
                notifica.getSegnalazione(),
                control.apriSegnalazioneDaNotifica(
                        notifica,
                        organizzatore
                )
        );
    }

    private void verificaStato(boolean letta) {
        assertEquals(letta, notifica.getLetta());

        assertEquals(
                letta,
                repository.ottieniNotificheRicevute(organizzatore)
                        .getFirst()
                        .getLetta()
        );

        assertEquals(
                StatoSegnalazione.DA_ESAMINARE,
                notifica.getSegnalazione().getStato()
        );

        assertNull(notifica.getSegnalazione().getEsito());
    }

    @ParameterizedTest
    @CsvSource({
            "false,false",
            "false,true",
            "true,false",
            "true,true"
    })
    void ripristinaLoStatoPrecedenteEConsenteNuovoTentativo(
            boolean jdbcAttivo,
            boolean letta
    ) {
        prepara(jdbcAttivo, letta);

        Long id = notifica.getId();
        var data = notifica.getDataOraCreazione();

        for (RuntimeException causa : List.of(
                new RuntimeException("Salvataggio fallito"),
                new IllegalArgumentException("Errore del repository"),
                new IllegalStateException("Repository non disponibile")
        )) {
            doThrow(causa).when(repository).salvaNotifica(any());

            IllegalStateException errore = assertThrows(
                    IllegalStateException.class,
                    this::apri
            );

            assertSame(causa, errore.getCause());

            assertEquals(
                    "La lettura della notifica non è stata registrata",
                    errore.getMessage()
            );

            verificaStato(letta);

            assertEquals(id, notifica.getId());
            assertEquals(data, notifica.getDataOraCreazione());
        }

        doCallRealMethod().when(repository).salvaNotifica(any());

        apri();
        verificaStato(true);
    }

    @Test
    void ripristinaDopoErroreSqlEConsenteNuovoTentativo() {
        prepara(true, false);

        JdbcClient jdbc = JdbcClient.create(database);

        jdbc.sql("""
                ALTER TABLE notifica_segnalazione
                ADD CONSTRAINT blocca_lettura CHECK (letta = FALSE)
                """).update();

        assertThrows(IllegalStateException.class, this::apri);
        verificaStato(false);

        jdbc.sql("""
                ALTER TABLE notifica_segnalazione
                DROP CONSTRAINT blocca_lettura
                """).update();

        apri();
        verificaStato(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ripristinaDopoCommitFallito(boolean letta) {
        prepara(true, letta);

        var hackathons = new JdbcHackathonRepository(database);

        var partecipazioni = new JdbcPartecipazioneRepository(
                database,
                hackathons
        );

        var guasto = new JdbcSegnalazioneRepository(
                dataSourceConCommitFallito(),
                hackathons,
                partecipazioni
        );

        var controlGuasto = new EsaminareSegnalazioneControl(
                guasto,
                partecipazioni
        );

        assertThrows(
                IllegalStateException.class,
                () -> controlGuasto.apriSegnalazioneDaNotifica(
                        notifica,
                        organizzatore
                )
        );

        verificaStato(letta);

        apri();
        verificaStato(true);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ripristinaDopoRollbackEsternoAncheConDueAperture(boolean letta) {
        prepara(true, letta);

        var transazione = new TransactionTemplate(
                new JdbcTransactionManager(database)
        );

        transazione.executeWithoutResult(stato -> {
            apri();
            apri();
            stato.setRollbackOnly();
        });

        verificaStato(letta);

        apri();
        verificaStato(true);
    }

    @Test
    void rifiutaIlDestinatarioErratoPrimaDiModificareLaNotifica() {
        prepara(false, false);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.apriSegnalazioneDaNotifica(
                        notifica,
                        new Utente(999L)
                )
        );

        verificaStato(false);

        verify(repository, never()).salvaNotifica(any());
    }

    private DataSource dataSourceConCommitFallito() {
        return new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                Connection reale = database.getConnection();

                return (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (proxy, metodo, argomenti) -> {
                            if (metodo.getName().equals("commit")) {
                                throw new SQLException("Commit fallito");
                            }

                            try {
                                return metodo.invoke(reale, argomenti);
                            } catch (InvocationTargetException errore) {
                                throw errore.getCause();
                            }
                        }
                );
            }

            @Override
            public Connection getConnection(
                    String utente,
                    String password
            ) throws SQLException {
                return getConnection();
            }
        };
    }
}