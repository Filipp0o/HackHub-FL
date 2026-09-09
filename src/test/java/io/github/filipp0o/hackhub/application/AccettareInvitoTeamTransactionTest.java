package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.application.AccettareInvitoTeamControl.AccettazioneInvitoFallitaException;
import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.infrastructure.*;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.aop.support.AopUtils;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.*;

class AccettareInvitoTeamTransactionTest {

    private EmbeddedDatabase database;
    private AnnotationConfigApplicationContext context;
    private JdbcClient jdbc;
    private JdbcInvitoRepository inviti;
    private TeamConGuasto teams;
    private AccettareInvitoTeamControl control;
    private Utente creatore;
    private Utente destinatario;
    private Team team;
    private Invito invito;

    @BeforeEach
    void prepara() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();

        jdbc = JdbcClient.create(database);
        inviti = new JdbcInvitoRepository(jdbc);
        teams = new TeamConGuasto(database);

        context = new AnnotationConfigApplicationContext();
        context.register(ConfigTransazioni.class);

        context.registerBean(
                PlatformTransactionManager.class,
                () -> new JdbcTransactionManager(database)
        );
        context.registerBean(
                InvitoRepository.class, () -> inviti
        );
        context.registerBean(
                TeamRepository.class, () -> teams
        );
        context.registerBean(
                PartecipazioneRepository.class,
                InMemoryPartecipazioneRepository::new
        );
        context.registerBean(
                AccettareInvitoTeamControl.class,
                () -> new AccettareInvitoTeamControl(
                        context.getBean(InvitoRepository.class),
                        context.getBean(TeamRepository.class),
                        context.getBean(PartecipazioneRepository.class)
                )
        );

        context.refresh();
        control = context.getBean(AccettareInvitoTeamControl.class);

        var utenti = new JdbcUtenteRepository(jdbc);

        creatore = Utente.crea(
                "creatore@example.com", "hash-creatore"
        );
        destinatario = Utente.crea(
                "destinatario@example.com", "hash-destinatario"
        );

        utenti.salva(creatore);
        utenti.salva(destinatario);

        team = Team.crea("ByteBuilders", creatore, creatore);
        teams.salva(team);

        invito = Invito.crea(team, destinatario);
        inviti.salva(invito);

        teams.invitoDaVerificare = invito.getId();
    }

    @AfterEach
    void chiudi() {
        if (context != null) {
            context.close();
        }
        if (database != null) {
            database.shutdown();
        }
    }

    @Test
    void proxySpringConfermaInvitoEMembroNellaStessaTransazione() {
        assertTrue(AopUtils.isAopProxy(control));
        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );

        control.richiediAccettazioneInvito(destinatario, invito);

        assertTrue(teams.transazioneOsservata);
        assertTrue(teams.accettazioneOsservata);
        assertTrue(
                inviti.recuperaInvitiRicevuti(destinatario).isEmpty()
        );
        assertEquals(
                team.getId(),
                teams.recuperaTeam(destinatario).getId()
        );
        assertEquals(2, teams.recuperaTeam(creatore).numeroMembri());
        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );
    }

    @Test
    void erroreSqlDuranteSalvataggioTeamAnnullaAncheInvito() {
        teams.guastoDurante = true;

        AccettazioneInvitoFallitaException errore = assertThrows(
                AccettazioneInvitoFallitaException.class,
                () -> control.richiediAccettazioneInvito(
                        destinatario, invito
                )
        );

        assertEquals(
                "Accettazione non completata",
                errore.getMessage()
        );
        assertNotNull(errore.getCause());
        assertEquals("Impossibile salvare il team", errore.getCause().getMessage());
        assertNotNull(errore.getCause().getCause());

        verificaRollback();
    }

    @Test
    void erroreDopoSalvataggioTeamAnnullaEntrambeLeScritture() {
        teams.guastoDopo = true;

        AccettazioneInvitoFallitaException errore = assertThrows(
                AccettazioneInvitoFallitaException.class,
                () -> control.richiediAccettazioneInvito(
                        destinatario, invito
                )
        );

        assertEquals(
                "Guasto dopo il salvataggio del team",
                errore.getCause().getMessage()
        );

        verificaRollback();

        teams.guastoDopo = false;
        assertDoesNotThrow(() ->
                control.richiediAccettazioneInvito(destinatario, invito));
        assertTrue(inviti.recuperaInvitiRicevuti(destinatario).isEmpty());
        assertEquals(2, teams.recuperaTeam(creatore).numeroMembri());
    }

    private void verificaRollback() {
        assertTrue(teams.transazioneOsservata);
        assertTrue(teams.accettazioneOsservata);

        var ricevuti = new JdbcInvitoRepository(
                JdbcClient.create(database)
        ).recuperaInvitiRicevuti(destinatario);

        assertEquals(1, ricevuti.size());
        assertEquals(invito.getId(), ricevuti.get(0).getId());
        assertFalse(ricevuti.get(0).isAccettato());

        var riletturaTeam = new JdbcTeamRepository(database);

        assertEquals(
                1,
                riletturaTeam.recuperaTeam(creatore).numeroMembri()
        );
        assertFalse(
                riletturaTeam.verificaAppartenenzaTeam(destinatario)
        );
        assertFalse(
                TransactionSynchronizationManager
                        .isActualTransactionActive()
        );
    }

    @TestConfiguration
    @EnableTransactionManagement(proxyTargetClass = true)
    static class ConfigTransazioni {
    }

    static class TeamConGuasto extends JdbcTeamRepository {

        private final JdbcClient jdbc;
        private Long invitoDaVerificare;
        private boolean guastoDurante;
        private boolean guastoDopo;
        private boolean transazioneOsservata;
        private boolean accettazioneOsservata;

        TeamConGuasto(DataSource dataSource) {
            super(dataSource);
            jdbc = JdbcClient.create(dataSource);
        }

        @Override
        public void salva(Team team) {
            if (invitoDaVerificare != null) {
                transazioneOsservata =
                        TransactionSynchronizationManager
                                .isActualTransactionActive();

                accettazioneOsservata = jdbc.sql("""
                                SELECT accettato FROM invito WHERE id = :id
                                """)
                        .param("id", invitoDaVerificare)
                        .query(Boolean.class)
                        .single();
            }

            if (guastoDurante) {
                // Utente inesistente: provoca un errore reale di foreign key.
                team.aggiungiMembro(new Utente(999999L));
            }

            super.salva(team);

            if (guastoDopo) {
                throw new IllegalStateException(
                        "Guasto dopo il salvataggio del team"
                );
            }
        }
    }
}