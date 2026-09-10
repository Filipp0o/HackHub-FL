package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.application.EsaminareSegnalazioneControl.RegistrazioneDecisioneFallitaException;
import io.github.filipp0o.hackhub.domain.*;
import io.github.filipp0o.hackhub.presentation.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionOperations;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;

class EsaminareSegnalazioneRecoveryTest {

    private EmbeddedDatabase database;
    private Utente organizzatore;
    private Partecipazione partecipazione;
    private Segnalazione segnalazione;
    private PartecipazioneRepository partecipazioni;
    private SegnalazioneRepository segnalazioni;
    private EsaminareSegnalazioneControl control;

    @AfterEach
    void chiudi() {
        if (database != null) {
            database.shutdown();
        }
    }

    private void prepara(boolean jdbcAttivo, boolean commitFallito) {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();

        JdbcClient jdbc = JdbcClient.create(database);
        var utenti = new JdbcUtenteRepository(jdbc);

        organizzatore = Utente.crea(
                "organizzatore@example.com", "hash-organizzatore"
        );
        Utente mentore = Utente.crea(
                "mentore@example.com", "hash-mentore"
        );
        Utente giudice = Utente.crea(
                "giudice@example.com", "hash-giudice"
        );
        Utente responsabile = Utente.crea(
                "responsabile@example.com", "hash-responsabile"
        );

        List.of(organizzatore, mentore, giudice, responsabile)
                .forEach(utenti::salva);

        Team team = Team.crea("Team Alpha", responsabile, responsabile);
        new JdbcTeamRepository(database).salva(team);

        LocalDate oggi = LocalDate.now();
        var hackathons = new JdbcHackathonRepository(database);

        Hackathon h = Hackathon.crea(
                new DatiHackathon(
                        "HackHub", "Regolamento", "Criteri",
                        oggi.minusDays(4), oggi.minusDays(2), oggi.plusDays(2),
                        "Camerino", BigDecimal.TEN, 5
                ),
                organizzatore,
                giudice,
                List.of(mentore)
        );

        hackathons.salva(h);
        h.aggiornaStato(oggi);

        partecipazione = Partecipazione.crea(h, team);

        partecipazioni = spy(jdbcAttivo
                ? new JdbcPartecipazioneRepository(database, hackathons)
                : new InMemoryPartecipazioneRepository());

        partecipazioni.salva(partecipazione);

        segnalazione = Segnalazione.crea(
                mentore, partecipazione, "Violazione"
        );

        segnalazioni = spy(jdbcAttivo
                ? new JdbcSegnalazioneRepository(
                database, hackathons, partecipazioni
        )
                : new SegnalazioneRepositoryImpl());

        segnalazioni.salva(segnalazione);

        DataSource origine = commitFallito
                ? dataSourceConCommitFallito()
                : database;

        if (commitFallito) {
            partecipazioni = new JdbcPartecipazioneRepository(
                    origine, hackathons
            );
            segnalazioni = new JdbcSegnalazioneRepository(
                    origine, hackathons, partecipazioni
            );
        }

        var gestore = new JdbcTransactionManager(origine);
        gestore.setRollbackOnCommitFailure(true);

        TransactionOperations tx = jdbcAttivo
                ? new TransactionTemplate(gestore)
                : TransactionOperations.withoutTransaction();

        control = new EsaminareSegnalazioneControl(
                segnalazioni, partecipazioni, tx
        );
    }

    private DatiDecisioneSegnalazione esclusione() {
        return new DatiDecisioneSegnalazione(
                EsitoSegnalazione.VIOLAZIONE_CON_ESCLUSIONE,
                "Violazione confermata"
        );
    }

    private void verificaRipristino(StatoPartecipazione precedente) {
        assertAll(
                () -> assertEquals(
                        StatoSegnalazione.DA_ESAMINARE,
                        segnalazione.getStato()
                ),
                () -> assertNull(segnalazione.getEsito()),
                () -> assertNull(segnalazione.getMotivazione()),
                () -> assertNull(segnalazione.getEsaminatore()),
                () -> assertNull(segnalazione.getDataOraEsame()),
                () -> assertEquals(precedente, partecipazione.getStato())
        );

        Segnalazione riletta = segnalazioni
                .ottieniSegnalazioniDaEsaminare(organizzatore)
                .getFirst();

        assertEquals(segnalazione.getId(), riletta.getId());
        assertNull(riletta.getEsito());

        assertEquals(
                precedente,
                partecipazioni.recuperaPartecipazione(
                        partecipazione.getTeam(),
                        partecipazione.getHackathon()
                ).getStato()
        );
    }

    @ParameterizedTest
    @CsvSource({
            "false,false,ATTIVA",
            "false,true,ATTIVA",
            "false,false,ESCLUSA",
            "false,true,ESCLUSA",
            "true,false,ATTIVA",
            "true,true,ATTIVA",
            "true,false,ESCLUSA",
            "true,true,ESCLUSA"
    })
    void ripristinaDopoOgniSalvataggioFallitoEConsenteNuovoTentativo(
            boolean jdbcAttivo,
            boolean falliscePartecipazione,
            StatoPartecipazione precedente
    ) {
        prepara(jdbcAttivo, false);

        partecipazione.ripristinaStato(precedente);
        partecipazioni.salva(partecipazione);

        var causa = new IllegalArgumentException("Salvataggio fallito");

        if (falliscePartecipazione) {
            doThrow(causa).when(partecipazioni).salva(any());
        } else {
            doThrow(causa).when(segnalazioni).salva(any());
        }

        var errore = assertThrows(
                RegistrazioneDecisioneFallitaException.class,
                () -> control.registraDecisione(
                        segnalazione, organizzatore, esclusione()
                )
        );

        assertSame(causa, errore.getCause());
        verificaRipristino(precedente);

        if (falliscePartecipazione) {
            doCallRealMethod().when(partecipazioni).salva(any());
        } else {
            doCallRealMethod().when(segnalazioni).salva(any());
        }

        control.registraDecisione(
                segnalazione, organizzatore, esclusione()
        );

        assertEquals(
                StatoSegnalazione.ESAMINATA,
                segnalazione.getStato()
        );
        assertTrue(
                segnalazioni.ottieniSegnalazioniDaEsaminare(organizzatore)
                        .isEmpty()
        );
        assertEquals(
                StatoPartecipazione.ESCLUSA,
                partecipazioni.recuperaPartecipazione(
                        partecipazione.getTeam(),
                        partecipazione.getHackathon()
                ).getStato()
        );
    }

    @ParameterizedTest
    @EnumSource(EsitoSegnalazione.class)
    void registraTuttiGliEsitiEConservaUnaDecisionePreesistente(
            EsitoSegnalazione esito
    ) {
        prepara(true, false);

        var dati = new DatiDecisioneSegnalazione(
                esito, "Decisione motivata"
        );

        control.registraDecisione(segnalazione, organizzatore, dati);
        var data = segnalazione.getDataOraEsame();

        assertThrows(
                IllegalStateException.class,
                () -> control.registraDecisione(
                        segnalazione, organizzatore, esclusione()
                )
        );

        assertEquals(esito, segnalazione.getEsito());
        assertEquals(data, segnalazione.getDataOraEsame());

        StatoPartecipazione atteso =
                esito == EsitoSegnalazione.VIOLAZIONE_CON_ESCLUSIONE
                        ? StatoPartecipazione.ESCLUSA
                        : StatoPartecipazione.ATTIVA;

        assertEquals(
                atteso,
                partecipazioni.recuperaPartecipazione(
                        partecipazione.getTeam(),
                        partecipazione.getHackathon()
                ).getStato()
        );
    }

    @Test
    void ripristinaAncheQuandoFallisceIlCommit() {
        prepara(true, true);

        assertThrows(
                RegistrazioneDecisioneFallitaException.class,
                () -> control.registraDecisione(
                        segnalazione, organizzatore, esclusione()
                )
        );

        assertEquals(
                StatoSegnalazione.DA_ESAMINARE,
                segnalazione.getStato()
        );
        assertNull(segnalazione.getEsito());
        assertEquals(
                StatoPartecipazione.ATTIVA,
                partecipazione.getStato()
        );

        JdbcClient jdbc = JdbcClient.create(database);

        assertEquals(
                "DA_ESAMINARE",
                jdbc.sql("SELECT stato FROM segnalazione")
                        .query(String.class)
                        .single()
        );
        assertNull(
                jdbc.sql("SELECT esito FROM segnalazione")
                        .query((rs, i) -> rs.getString(1))
                        .list()
                        .getFirst()
        );
        assertEquals(
                "ATTIVA",
                jdbc.sql("SELECT stato FROM partecipazione")
                        .query(String.class)
                        .single()
        );
    }

    @Test
    void ripristinaSeLaTransazioneEsternaVieneAnnullata() {
        prepara(true, false);

        var esterna = new TransactionTemplate(
                new JdbcTransactionManager(database)
        );

        esterna.executeWithoutResult(stato -> {
            control.registraDecisione(
                    segnalazione, organizzatore, esclusione()
            );
            stato.setRollbackOnly();
        });

        verificaRipristino(StatoPartecipazione.ATTIVA);
    }

    @Test
    void restituisceHttp500ERipristinaPrimaDelNuovoTentativo()
            throws Exception {
        prepara(false, false);

        var sessione = new SessioneUtente();
        sessione.registra(organizzatore);

        var mvc = standaloneSetup(
                new EsaminareSegnalazioneBoundary(control, sessione)
        )
                .setControllerAdvice(new ErroriRichiestaHandler())
                .build();

        doThrow(new IllegalArgumentException("Repository non disponibile"))
                .when(segnalazioni)
                .salva(any());

        String json = """
                {
                    "esito": "VIOLAZIONE_CON_ESCLUSIONE",
                    "motivazione": "Violazione confermata"
                }
                """;

        String percorso = "/api/segnalazioni/"
                + segnalazione.getId()
                + "/decisione";

        mvc.perform(
                        post(percorso)
                                .contentType("application/json")
                                .content(json)
                )
                .andExpect(status().isInternalServerError())
                .andExpect(
                        jsonPath("$.messaggio")
                                .value("La decisione non è stata registrata")
                );

        verificaRipristino(StatoPartecipazione.ATTIVA);

        doCallRealMethod().when(segnalazioni).salva(any());

        mvc.perform(
                        post(percorso)
                                .contentType("application/json")
                                .content(json)
                )
                .andExpect(status().isNoContent());
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