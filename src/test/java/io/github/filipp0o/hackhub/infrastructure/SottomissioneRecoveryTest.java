package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.application.AggiornareSottomissioneControl.AggiornamentoSottomissioneFallitoException;
import io.github.filipp0o.hackhub.application.InviareSottomissioneControl.RegistrazioneSottomissioneFallitaException;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabase;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class SottomissioneRecoveryTest {

    private static final String PRECEDENTE = "Originale";
    private static final String NUOVO = "Versione nuova";

    private EmbeddedDatabase database;
    private DataSource dataSource;
    private JdbcClient jdbc;

    private boolean fallisceCommit;
    private boolean erroreCommitDuranteSalvataggio;
    private RuntimeException erroreSalvataggio;

    private Utente membro;
    private Hackathon hackathon;
    private Partecipazione partecipazione;
    private Sottomissione originale;

    private PartecipazioneRepository partecipazioni;
    private SottomissioneRepository sottomissioni;

    private InviareSottomissioneControl invio;
    private AggiornareSottomissioneControl aggiornamento;

    private final List<Sottomissione> tentativi = new ArrayList<>();

    @AfterEach
    void chiudi() {
        if (database != null) {
            database.shutdown();
        }
    }

    private void prepara(boolean jdbcAttivo, boolean modifica) {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();

        dataSource = dataSourceControllato();
        jdbc = JdbcClient.create(dataSource);

        var utenti = new JdbcUtenteRepository(jdbc);

        membro = Utente.crea(
                "membro@example.com",
                "hash-membro"
        );

        Utente organizzatore = Utente.crea(
                "organizzatore@example.com",
                "hash-organizzatore"
        );

        Utente giudice = Utente.crea(
                "giudice@example.com",
                "hash-giudice"
        );

        Utente mentore = Utente.crea(
                "mentore@example.com",
                "hash-mentore"
        );

        List.of(
                membro,
                organizzatore,
                giudice,
                mentore
        ).forEach(utenti::salva);

        Team team = Team.crea("Team Alpha", membro, membro);

        var teams = new JdbcTeamRepository(dataSource);
        teams.salva(team);

        LocalDate oggi = LocalDate.now();

        hackathon = Hackathon.crea(
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

        var hackathons = new JdbcHackathonRepository(dataSource);
        hackathons.salva(hackathon);

        hackathon.aggiornaStato(oggi);

        partecipazione = Partecipazione.crea(hackathon, team);

        partecipazioni = jdbcAttivo
                ? new JdbcPartecipazioneRepository(dataSource, hackathons)
                : new InMemoryPartecipazioneRepository();

        partecipazioni.salva(partecipazione);

        sottomissioni = spy(jdbcAttivo
                ? new JdbcSottomissioneRepository(dataSource, partecipazioni)
                : new SottomissioneRepositoryImpl());

        if (modifica) {
            originale = Sottomissione.crea(partecipazione, PRECEDENTE);
            sottomissioni.salva(originale);
        }

        doAnswer(chiamata -> {
            Sottomissione tentativo = chiamata.getArgument(0);
            tentativi.add(tentativo);

            if (erroreSalvataggio != null) {
                throw erroreSalvataggio;
            }

            fallisceCommit = erroreCommitDuranteSalvataggio;

            try {
                return chiamata.callRealMethod();
            } finally {
                fallisceCommit = false;
            }
        }).when(sottomissioni).salva(any());

        invio = new InviareSottomissioneControl(
                teams,
                partecipazioni,
                sottomissioni
        );

        aggiornamento = new AggiornareSottomissioneControl(
                teams,
                partecipazioni,
                sottomissioni
        );
    }

    private void esegui(boolean modifica) {
        if (modifica) {
            aggiornamento.aggiornaSottomissione(
                    membro,
                    hackathon,
                    NUOVO
            );
        } else {
            invio.inviaSottomissione(partecipazione, NUOVO);
        }
    }

    private IllegalStateException verificaErrore(boolean modifica) {
        return modifica
                ? assertThrows(
                AggiornamentoSottomissioneFallitoException.class,
                () -> esegui(true)
        )
                : assertThrows(
                RegistrazioneSottomissioneFallitaException.class,
                () -> esegui(false)
        );
    }

    private void verificaRipristino(boolean modifica, boolean jdbcAttivo) {
        Sottomissione tentativo = tentativi.getLast();

        if (modifica) {
            assertEquals(PRECEDENTE, tentativo.getContenuto());
            assertEquals(originale.getId(), tentativo.getId());

            assertSame(
                    tentativo,
                    tentativo.getPartecipazione().getSottomissione()
            );

            assertEquals(
                    PRECEDENTE,
                    sottomissioni.recuperaSottomissione(partecipazione)
                            .getContenuto()
            );
        } else {
            assertNull(partecipazione.getSottomissione());
            assertNull(tentativo.getId());

            assertThrows(
                    IllegalStateException.class,
                    () -> sottomissioni.recuperaSottomissione(partecipazione)
            );
        }

        assertEquals(
                StatoPartecipazione.ATTIVA,
                partecipazione.getStato()
        );

        if (jdbcAttivo) {
            assertEquals(
                    modifica ? 1 : 0,
                    jdbc.sql("SELECT COUNT(*) FROM sottomissione")
                            .query(Integer.class)
                            .single()
            );

            if (modifica) {
                assertEquals(
                        PRECEDENTE,
                        jdbc.sql("SELECT contenuto FROM sottomissione")
                                .query(String.class)
                                .single()
                );
            }
        }
    }

    private void verificaNuovoTentativo(boolean modifica) {
        assertDoesNotThrow(() -> esegui(modifica));

        Sottomissione salvata =
                sottomissioni.recuperaSottomissione(partecipazione);

        assertEquals(NUOVO, salvata.getContenuto());
        assertNotNull(salvata.getId());

        if (modifica) {
            assertEquals(originale.getId(), salvata.getId());
        }
    }

    @ParameterizedTest
    @CsvSource({
            "false,false",
            "false,true",
            "true,false",
            "true,true"
    })
    void ripristinaDopoErroreDelRepositoryEConsenteNuovoTentativo(
            boolean jdbcAttivo,
            boolean modifica
    ) {
        prepara(jdbcAttivo, modifica);

        for (RuntimeException causa : List.of(
                new RuntimeException("Salvataggio fallito"),
                new IllegalArgumentException("Errore del repository"),
                new IllegalStateException("Repository non disponibile")
        )) {
            erroreSalvataggio = causa;

            assertSame(
                    causa,
                    verificaErrore(modifica).getCause()
            );

            verificaRipristino(modifica, jdbcAttivo);
        }

        erroreSalvataggio = null;

        verificaNuovoTentativo(modifica);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ripristinaDopoErroreSqlEConsenteNuovoTentativo(boolean modifica) {
        prepara(true, modifica);

        jdbc.sql("""
                ALTER TABLE sottomissione
                ADD CONSTRAINT blocca_nuovo_contenuto
                CHECK (CAST(contenuto AS VARCHAR) <> 'Versione nuova')
                """).update();

        verificaErrore(modifica);
        verificaRipristino(modifica, true);

        jdbc.sql("""
                ALTER TABLE sottomissione
                DROP CONSTRAINT blocca_nuovo_contenuto
                """).update();

        verificaNuovoTentativo(modifica);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ripristinaDopoCommitFallitoEConsenteNuovoTentativo(
            boolean modifica
    ) {
        prepara(true, modifica);

        erroreCommitDuranteSalvataggio = true;

        verificaErrore(modifica);
        verificaRipristino(modifica, true);

        erroreCommitDuranteSalvataggio = false;

        verificaNuovoTentativo(modifica);
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void ripristinaDopoRollbackEsterno(boolean modifica) {
        prepara(true, modifica);

        var transazione = new TransactionTemplate(
                new JdbcTransactionManager(dataSource)
        );

        transazione.executeWithoutResult(stato -> {
            esegui(modifica);
            stato.setRollbackOnly();
        });

        verificaRipristino(modifica, true);
        verificaNuovoTentativo(modifica);
    }

    @Test
    void ripristinaIlPrimoContenutoDopoDueAggiornamentiDelloStessoOggetto() {
        prepara(true, true);

        doReturn(originale)
                .when(sottomissioni)
                .recuperaSottomissione(any());

        var transazione = new TransactionTemplate(
                new JdbcTransactionManager(dataSource)
        );

        transazione.executeWithoutResult(stato -> {
            aggiornamento.aggiornaSottomissione(
                    membro,
                    hackathon,
                    "Seconda versione"
            );

            aggiornamento.aggiornaSottomissione(
                    membro,
                    hackathon,
                    "Terza versione"
            );

            stato.setRollbackOnly();
        });

        assertEquals(PRECEDENTE, originale.getContenuto());

        assertEquals(
                PRECEDENTE,
                jdbc.sql("SELECT contenuto FROM sottomissione")
                        .query(String.class)
                        .single()
        );
    }

    @Test
    void annullaSoloLaSottomissioneDelTentativoFallito() {
        prepara(false, false);

        Sottomissione prima = Sottomissione.crea(
                partecipazione,
                PRECEDENTE
        );

        partecipazione.annullaSottomissioneNonRegistrata(prima);

        assertNull(partecipazione.getSottomissione());

        Sottomissione seconda = Sottomissione.crea(
                partecipazione,
                NUOVO
        );

        partecipazione.annullaSottomissioneNonRegistrata(prima);

        assertSame(seconda, partecipazione.getSottomissione());
        assertEquals(NUOVO, seconda.getContenuto());
    }

    @Test
    void conservaLaSottomissionePreesistenteQuandoRifiutaUnNuovoInvio() {
        prepara(false, true);

        IllegalStateException errore = assertThrows(
                IllegalStateException.class,
                () -> esegui(false)
        );

        assertEquals(IllegalStateException.class, errore.getClass());
        assertSame(originale, partecipazione.getSottomissione());
        assertEquals(PRECEDENTE, originale.getContenuto());
        assertTrue(tentativi.isEmpty());
    }

    private DataSource dataSourceControllato() {
        return new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                Connection reale = database.getConnection();

                return (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (proxy, metodo, argomenti) -> {
                            if (fallisceCommit
                                    && metodo.getName().equals("commit")) {
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