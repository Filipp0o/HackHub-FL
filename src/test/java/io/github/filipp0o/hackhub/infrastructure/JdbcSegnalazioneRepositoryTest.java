package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.SegnalareViolazioneControl;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.AbstractDataSource;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcSegnalazioneRepositoryTest {

    private EmbeddedDatabase database;
    private DataSource origine;
    private JdbcClient jdbc;
    private JdbcHackathonRepository hackathons;
    private JdbcPartecipazioneRepository partecipazioni;
    private JdbcSegnalazioneRepository repository;
    private Utente organizzatore, mentore;
    private Partecipazione partecipazione;

    @BeforeEach
    void prepara() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();

        collega(database);
        creaScenario();
    }

    @AfterEach
    void chiudi() {
        database.shutdown();
    }

    private void collega(DataSource dataSource) {
        origine = dataSource;
        jdbc = JdbcClient.create(dataSource);
        hackathons = new JdbcHackathonRepository(dataSource);
        partecipazioni = new JdbcPartecipazioneRepository(dataSource, hackathons);
        repository = new JdbcSegnalazioneRepository(
                dataSource, hackathons, partecipazioni
        );
    }

    private void creaScenario() {
        var utenti = new JdbcUtenteRepository(jdbc);

        organizzatore = Utente.crea(
                "organizzatore@example.com", "hash-organizzatore"
        );
        mentore = Utente.crea("mentore@example.com", "hash-mentore");
        Utente giudice = Utente.crea("giudice@example.com", "hash-giudice");
        Utente responsabile = Utente.crea(
                "responsabile@example.com", "hash-responsabile"
        );

        List.of(organizzatore, mentore, giudice, responsabile)
                .forEach(utenti::salva);

        Team team = Team.crea("Team Alpha", responsabile, responsabile);
        new JdbcTeamRepository(origine).salva(team);

        LocalDate oggi = LocalDate.now();
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
        partecipazione = Partecipazione.crea(h, team);
        partecipazioni.salva(partecipazione);
        h.aggiornaStato(oggi);
    }

    private Segnalazione nuova() {
        return Segnalazione.crea(
                mentore, partecipazione, "Violazione del regolamento"
        );
    }

    private long conta(String tabella) {
        return jdbc.sql("SELECT COUNT(*) FROM " + tabella)
                .query(Long.class)
                .single();
    }

    @ParameterizedTest
    @EnumSource(EsitoSegnalazione.class)
    void conservaDecisioneDateAssociazioniELetturaSenzaDuplicati(
            EsitoSegnalazione esito
    ) {
        Segnalazione s = nuova();
        NotificaSegnalazione n = NotificaSegnalazione.crea(s, organizzatore);

        repository.salvaConNotifica(s, n);
        repository.salvaConNotifica(s, n);

        Segnalazione riletta = repository
                .ottieniSegnalazioniDaEsaminare(organizzatore)
                .getFirst();

        assertNotSame(s, riletta);
        assertEquals(s.getId(), riletta.getId());
        assertEquals(s.getDataOraCreazione(), riletta.getDataOraCreazione());
        assertEquals(
                mentore.recuperaEmail(),
                riletta.getMentoreSegnalante().recuperaEmail()
        );
        assertEquals(partecipazione.getId(), riletta.getPartecipazione().getId());
        assertSame(riletta, riletta.getNotificaSegnalazione().getSegnalazione());
        assertFalse(riletta.getNotificaSegnalazione().getLetta());
        assertTrue(repository.ottieniSegnalazioniDaEsaminare(mentore).isEmpty());
        assertTrue(repository.ottieniNotificheRicevute(mentore).isEmpty());

        n.segnaComeLetta();
        repository.salvaNotifica(n);

        s.registraEsame(
                new DatiDecisioneSegnalazione(esito, "Decisione motivata"),
                organizzatore
        );
        repository.salva(s);

        assertTrue(
                repository.ottieniSegnalazioniDaEsaminare(organizzatore).isEmpty()
        );

        NotificaSegnalazione letta = repository
                .ottieniNotificheRicevute(organizzatore)
                .getFirst();

        assertEquals(n.getId(), letta.getId());
        assertEquals(n.getDataOraCreazione(), letta.getDataOraCreazione());
        assertTrue(letta.getLetta());
        assertEquals(esito, letta.getSegnalazione().getEsito());
        assertEquals(s.getDataOraEsame(), letta.getSegnalazione().getDataOraEsame());
        assertEquals("Decisione motivata", letta.getSegnalazione().getMotivazione());
        assertEquals(
                organizzatore.recuperaEmail(),
                letta.getSegnalazione().getEsaminatore().recuperaEmail()
        );
        assertEquals(1, conta("segnalazione"));
        assertEquals(1, conta("notifica_segnalazione"));
    }

    @Test
    void fallimentoDellaNotificaAnnullaEntrambeLeScrittureEConsenteUnNuovoTentativo() {
        Segnalazione s = nuova();
        NotificaSegnalazione n = NotificaSegnalazione.crea(s, organizzatore);

        jdbc.sql("""
                ALTER TABLE notifica_segnalazione
                ADD CONSTRAINT errore_prova CHECK (letta = TRUE)
                """).update();

        assertThrows(
                IllegalStateException.class,
                () -> repository.salvaConNotifica(s, n)
        );

        assertNull(s.getId());
        assertNull(n.getId());
        assertEquals(0, conta("segnalazione"));
        assertEquals(0, conta("notifica_segnalazione"));

        jdbc.sql("""
                ALTER TABLE notifica_segnalazione
                DROP CONSTRAINT errore_prova
                """).update();

        repository.salvaConNotifica(s, n);

        assertNotNull(s.getId());
        assertNotNull(n.getId());
    }

    @Test
    void assegnaGliIdSoloAlCommitEsterno() {
        Segnalazione s = nuova();
        NotificaSegnalazione n = NotificaSegnalazione.crea(s, organizzatore);
        var tx = new TransactionTemplate(new JdbcTransactionManager(origine));

        tx.executeWithoutResult(stato -> {
            repository.salvaConNotifica(s, n);

            assertNull(s.getId());
            assertNull(n.getId());
            assertThrows(
                    IllegalStateException.class,
                    () -> repository.salvaConNotifica(s, n)
            );

            stato.setRollbackOnly();
        });

        assertNull(s.getId());
        assertNull(n.getId());
        assertEquals(0, conta("segnalazione"));

        tx.executeWithoutResult(stato -> repository.salvaConNotifica(s, n));

        assertNotNull(s.getId());
        assertNotNull(n.getId());
    }

    @Test
    void rifiutaAssociazioniDiverseEIdentificativiInesistenti() {
        Segnalazione s = nuova();
        NotificaSegnalazione n = NotificaSegnalazione.crea(
                nuova(), organizzatore
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> repository.salvaConNotifica(s, n)
        );

        s.assegnaId(99999L);

        assertThrows(IllegalStateException.class, () -> repository.salva(s));
        assertEquals(0, conta("segnalazione"));
    }

    @Test
    void uc08SalvaUnaSegnalazioneConNotificaRileggibile() {
        new SegnalareViolazioneControl(hackathons, partecipazioni, repository)
                .registraSegnalazioneConNotifica(
                        mentore, partecipazione, "Violazione"
                );

        assertEquals(
                1,
                repository.ottieniSegnalazioniDaEsaminare(organizzatore).size()
        );
        assertNotNull(
                repository.ottieniNotificheRicevute(organizzatore)
                        .getFirst()
                        .getId()
        );
    }

    @Test
    void riapreIlDatabaseERicostruisceAncheSegnalazioniStoriche(
            @TempDir Path cartella
    ) {
        String url = "jdbc:h2:file:"
                + cartella.resolve("segnalazioni").toAbsolutePath();

        var prima = new DriverManagerDataSource(url, "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql"))
                .execute(prima);

        collega(prima);
        creaScenario();

        Segnalazione s = nuova();
        NotificaSegnalazione n = NotificaSegnalazione.crea(s, organizzatore);

        repository.salvaConNotifica(s, n);
        n.segnaComeLetta();
        repository.salvaNotifica(n);

        jdbc.sql("""
                        UPDATE hackathon
                        SET tipo_stato = 'CONCLUSO', vincitrice_id = :id
                        """)
                .param("id", partecipazione.getId())
                .update();

        jdbc.sql("SHUTDOWN").update();

        collega(new DriverManagerDataSource(url, "sa", ""));

        NotificaSegnalazione letta = repository
                .ottieniNotificheRicevute(organizzatore)
                .getFirst();

        assertEquals(n.getId(), letta.getId());
        assertTrue(letta.getLetta());
        assertEquals(
                s.getDataOraCreazione(),
                letta.getSegnalazione().getDataOraCreazione()
        );
        assertFalse(
                letta.getSegnalazione().getPartecipazione()
                        .getHackathon().consenteSegnalazioni()
        );
    }

    @Test
    void memoriaAssegnaIdEAggiornaSenzaDuplicati() {
        var memoria = new SegnalazioneRepositoryImpl();
        Segnalazione s = nuova();
        NotificaSegnalazione n = NotificaSegnalazione.crea(s, organizzatore);

        memoria.salvaConNotifica(s, n);
        memoria.salvaConNotifica(s, n);
        n.segnaComeLetta();
        memoria.salvaNotifica(n);

        assertNotNull(s.getId());
        assertNotNull(n.getId());
        assertEquals(
                1,
                memoria.ottieniSegnalazioniDaEsaminare(organizzatore).size()
        );
        assertEquals(List.of(n), memoria.ottieniNotificheRicevute(organizzatore));
        assertTrue(memoria.ottieniNotificheRicevute(mentore).isEmpty());
    }

    @Test
    void fallimentoDelCommitNonAssegnaIdENonLasciaRighe() {
        DataSource difettosa = new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                Connection reale = origine.getConnection();

                return (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(),
                        new Class<?>[]{Connection.class},
                        (proxy, metodo, argomenti) -> {
                            if (metodo.getName().equals("commit")) {
                                throw new SQLException("Commit fallito");
                            }

                            try {
                                return metodo.invoke(reale, argomenti);
                            } catch (InvocationTargetException e) {
                                throw e.getCause();
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

        var difettoso = new JdbcSegnalazioneRepository(
                difettosa, hackathons, partecipazioni
        );
        Segnalazione s = nuova();
        NotificaSegnalazione n = NotificaSegnalazione.crea(s, organizzatore);

        assertThrows(
                IllegalStateException.class,
                () -> difettoso.salvaConNotifica(s, n)
        );

        assertNull(s.getId());
        assertNull(n.getId());
        assertEquals(0, conta("segnalazione"));
        assertEquals(0, conta("notifica_segnalazione"));
    }

    @Test
    void rifiutaRipristinoIncoerenteEIdentificativiNonValidi() {
        Segnalazione s = nuova();

        assertThrows(
                IllegalArgumentException.class,
                () -> Segnalazione.ricostruisci(
                        new DatiRipristinoSegnalazione(
                                1L, mentore, partecipazione, "Violazione",
                                s.getDataOraCreazione(),
                                StatoSegnalazione.ESAMINATA,
                                null, null, null, null
                        )
                )
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> Segnalazione.ricostruisci(
                        new DatiRipristinoSegnalazione(
                                1L, mentore, partecipazione, "Violazione",
                                s.getDataOraCreazione(),
                                StatoSegnalazione.DA_ESAMINARE,
                                EsitoSegnalazione.ARCHIVIATA,
                                null, null, null
                        )
                )
        );

        assertThrows(IllegalArgumentException.class, () -> s.assegnaId(0L));
        s.assegnaId(1L);
        assertThrows(IllegalStateException.class, () -> s.assegnaId(2L));

        assertThrows(
                IllegalArgumentException.class,
                () -> NotificaSegnalazione.ricostruisci(
                        -1L, s, organizzatore, s.getDataOraCreazione(), false
                )
        );

        assertNull(s.getNotificaSegnalazione());
    }
}