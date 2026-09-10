package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl;
import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl.RegistrazioneValutazioneFallitaException;
import io.github.filipp0o.hackhub.domain.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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

class JdbcSottomissioneValutazioneTest {

    private EmbeddedDatabase database;
    private DataSource dataSource;
    private JdbcClient jdbc;
    private JdbcHackathonRepository hackathons;
    private JdbcPartecipazioneRepository partecipazioni;
    private JdbcSottomissioneRepository sottomissioni;
    private JdbcValutazioneRepository valutazioni;
    private Utente organizzatore, giudice, mentore, responsabile;
    private Team team;

    @BeforeEach
    void prepara() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        collega(database);
        creaAccountETeam();
    }

    @AfterEach
    void chiudi() {
        database.shutdown();
    }

    private void collega(DataSource origine) {
        dataSource = origine;
        jdbc = JdbcClient.create(origine);
        hackathons = new JdbcHackathonRepository(origine);
        partecipazioni = new JdbcPartecipazioneRepository(origine, hackathons);
        sottomissioni = new JdbcSottomissioneRepository(origine, partecipazioni);
        valutazioni = new JdbcValutazioneRepository(origine);
    }

    private void creaAccountETeam() {
        var utenti = new JdbcUtenteRepository(jdbc);
        organizzatore = Utente.crea("organizzatore@example.com", "hash-organizzatore");
        giudice = Utente.crea("giudice@example.com", "hash-giudice");
        mentore = Utente.crea("mentore@example.com", "hash-mentore");
        responsabile = Utente.crea("responsabile@example.com", "hash-responsabile");
        List.of(organizzatore, giudice, mentore, responsabile).forEach(utenti::salva);
        team = Team.crea("ByteBuilders", responsabile, responsabile);
        new JdbcTeamRepository(dataSource).salva(team);
    }

    private Partecipazione creaPartecipazione() {
        LocalDate inizio = LocalDate.now().minusDays(10);
        DatiHackathon dati = new DatiHackathon(
                "HackHub", "Regolamento", "Criteri", inizio.minusDays(2),
                inizio, inizio.plusDays(2), "Camerino", new BigDecimal("100.50"), 5
        );
        Hackathon h = Hackathon.crea(dati, organizzatore, giudice, List.of(mentore));
        hackathons.salva(h);
        Partecipazione p = Partecipazione.crea(h, team);
        partecipazioni.salva(p);
        return p;
    }

    private Sottomissione salvaSottomissione(Partecipazione p) {
        Sottomissione s = Sottomissione.crea(p, "Soluzione originale");
        sottomissioni.salva(s);
        return s;
    }

    private DatiValutazione datiValutazione() {
        return new DatiValutazione("Buon lavoro", new BigDecimal("8.5"));
    }

    private ValutareSottomissioneControl control(JdbcValutazioneRepository repository) {
        return new ValutareSottomissioneControl(hackathons, partecipazioni, repository);
    }

    private long conta(String tabella) {
        return jdbc.sql("SELECT COUNT(*) FROM " + tabella).query(Long.class).single();
    }

    @Test
    void salvaAggiornaERileggeSenzaDuplicareLaSottomissione() {
        Partecipazione p = creaPartecipazione();
        Sottomissione s = salvaSottomissione(p);
        Long id = s.getId();
        s.aggiornaContenuto("Soluzione corretta");
        sottomissioni.salva(s);
        sottomissioni.salva(s);

        Sottomissione letta = sottomissioni.recuperaSottomissione(p);
        assertNotNull(id);
        assertNotSame(s, letta);
        assertEquals(id, letta.getId());
        assertEquals("Soluzione corretta", letta.getContenuto());
        assertSame(letta, letta.getPartecipazione().getSottomissione());
        assertEquals(responsabile.getId(), letta.getPartecipazione().getTeam().getResponsabile().getId());
        assertEquals(id, partecipazioni.recuperaPartecipazione(team, p.getHackathon()).getSottomissione().getId());
        assertEquals(1, conta("sottomissione"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "8.5", "8.123456789012345678901234567890123456789", "10"})
    void uc12RegistraUnaValutazioneRileggibileConPunteggioEDataEsatti(String punteggio) {
        Partecipazione p = creaPartecipazione();
        Sottomissione s = salvaSottomissione(p);
        var control = control(valutazioni);
        assertEquals(1, control.avviaValutazioneSottomissione(giudice).size());
        control.confermaValutazione(s, giudice, new DatiValutazione("Ottimo lavoro", new BigDecimal(punteggio)));
        Valutazione v = s.getValutazione();
        valutazioni.salva(v);

        Sottomissione letta = sottomissioni.recuperaSottomissione(p);
        Valutazione riletta = letta.getValutazione();
        assertNotNull(v.getId());
        assertNotSame(v, riletta);
        assertEquals(v.getId(), riletta.getId());
        assertEquals(0, v.getPunteggio().compareTo(riletta.getPunteggio()));
        assertEquals(v.getDataOra(), riletta.getDataOra());
        assertEquals("Ottimo lavoro", riletta.getGiudizio());
        assertEquals("giudice@example.com", riletta.getGiudice().recuperaEmail());
        assertSame(letta, riletta.getSottomissione());
        assertTrue(control.avviaValutazioneSottomissione(giudice).isEmpty());
        assertTrue(control.selezionaHackathon(p.getHackathon()).isEmpty());
        assertThrows(IllegalStateException.class,
                () -> control.confermaValutazione(letta, giudice, datiValutazione()));
        assertEquals(1, conta("valutazione"));
    }

    @Test
    void rifiutaSottomissioniAssentiEDuplicateEAssociazioniErrate() {
        Partecipazione p = creaPartecipazione();
        assertThrows(IllegalStateException.class, () -> sottomissioni.recuperaSottomissione(p));
        Partecipazione nonSalvata = Partecipazione.crea(p.getHackathon(), team);
        assertThrows(IllegalStateException.class, () -> sottomissioni.recuperaSottomissione(nonSalvata));
        Sottomissione senzaPartecipazioneSalvata = Sottomissione.crea(nonSalvata, "Nuova");
        assertThrows(IllegalArgumentException.class, () -> sottomissioni.salva(senzaPartecipazioneSalvata));
        Sottomissione s = salvaSottomissione(p);
        Partecipazione copia = Partecipazione.ricostruisci(p.getId(), p.getHackathon(), team, p.getStato());
        Sottomissione duplicata = Sottomissione.crea(copia, "Duplicata");
        assertThrows(IllegalStateException.class, () -> sottomissioni.salva(duplicata));
        assertNull(duplicata.getId());

        Partecipazione altra = creaPartecipazione();
        Sottomissione errata = Sottomissione.ricostruisci(s.getId(), altra, "Non deve sovrascrivere");
        assertThrows(IllegalStateException.class, () -> sottomissioni.salva(errata));
        Partecipazione idErrato = Partecipazione.ricostruisci(999L, p.getHackathon(), team, p.getStato());
        assertThrows(IllegalStateException.class, () -> sottomissioni.recuperaSottomissione(idErrato));
        Sottomissione assente = Sottomissione.ricostruisci(999L, idErrato, "Assente");
        assertThrows(IllegalStateException.class, () -> sottomissioni.salva(assente));
        assertEquals("Soluzione originale", sottomissioni.recuperaSottomissione(p).getContenuto());
        assertEquals(1, conta("sottomissione"));
    }

    @Test
    void rifiutaValutazioniDuplicateEUpdateConAssociazioniErrate() {
        Partecipazione p = creaPartecipazione();
        Sottomissione s = salvaSottomissione(p);
        control(valutazioni).confermaValutazione(s, giudice, datiValutazione());
        Valutazione v = s.getValutazione();
        Partecipazione copia = Partecipazione.ricostruisci(p.getId(), p.getHackathon(), team, p.getStato());
        Sottomissione duplicata = Sottomissione.ricostruisci(s.getId(), copia, s.getContenuto());
        Valutazione seconda = Valutazione.crea(duplicata, giudice, datiValutazione());
        assertThrows(IllegalStateException.class, () -> valutazioni.salva(seconda));
        assertNull(seconda.getId());

        Sottomissione altra = salvaSottomissione(creaPartecipazione());
        Valutazione errata = Valutazione.ricostruisci(v.getId(), altra, giudice, datiValutazione(), v.getDataOra());
        assertThrows(IllegalStateException.class, () -> valutazioni.salva(errata));
        assertNull(sottomissioni.recuperaSottomissione(altra.getPartecipazione()).getValutazione());
        assertEquals(v.getId(), sottomissioni.recuperaSottomissione(p).getValutazione().getId());
        assertEquals(1, conta("valutazione"));
    }

    @Test
    void erroreSqlUc12RipristinaLaSottomissioneEConsenteUnNuovoTentativo() {
        Partecipazione p = creaPartecipazione();
        Sottomissione s = salvaSottomissione(p);
        jdbc.sql("ALTER TABLE valutazione ADD CONSTRAINT prova_errore CHECK (giudice_id < 0)").update();
        RegistrazioneValutazioneFallitaException errore = assertThrows(
                RegistrazioneValutazioneFallitaException.class,
                () -> control(valutazioni).confermaValutazione(s, giudice, datiValutazione())
        );
        assertNotNull(errore.getCause());
        assertNull(s.getValutazione());
        assertNull(sottomissioni.recuperaSottomissione(p).getValutazione());
        assertEquals(0, conta("valutazione"));
        jdbc.sql("ALTER TABLE valutazione DROP CONSTRAINT prova_errore").update();
        control(valutazioni).confermaValutazione(s, giudice, datiValutazione());
        assertNotNull(sottomissioni.recuperaSottomissione(p).getValutazione().getId());
        assertEquals(1, conta("valutazione"));
    }

    @Test
    void erroreAlCommitNonAssegnaIdENonConservaScritture() {
        Partecipazione p = creaPartecipazione();
        Sottomissione s = Sottomissione.crea(p, "Originale");
        DataSource guasto = dataSourceConCommitFallito();
        var repositoryGuasto = new JdbcSottomissioneRepository(guasto, partecipazioni);
        assertThrows(IllegalStateException.class, () -> repositoryGuasto.salva(s));
        assertNull(s.getId());
        assertEquals(0, conta("sottomissione"));
        sottomissioni.salva(s);
        s.aggiornaContenuto("Aggiornamento fallito");
        assertThrows(IllegalStateException.class, () -> repositoryGuasto.salva(s));
        assertEquals("Originale", sottomissioni.recuperaSottomissione(p).getContenuto());

        Valutazione tentativo = Valutazione.crea(s, giudice, datiValutazione());
        assertThrows(IllegalStateException.class,
                () -> new JdbcValutazioneRepository(guasto).salva(tentativo));
        assertNull(tentativo.getId());
        s.annullaValutazioneNonRegistrata(tentativo);
        assertThrows(RegistrazioneValutazioneFallitaException.class,
                () -> control(new JdbcValutazioneRepository(guasto))
                        .confermaValutazione(s, giudice, datiValutazione()));
        assertNull(s.getValutazione());
        assertEquals(0, conta("valutazione"));
        control(valutazioni).confermaValutazione(s, giudice, datiValutazione());
        assertNotNull(sottomissioni.recuperaSottomissione(p).getValutazione().getId());
    }

    @Test
    void rollbackEsternoAnnullaGliInserimentiSenzaAssegnareId() {
        Sottomissione s = Sottomissione.crea(creaPartecipazione(), "Soluzione");
        var esterna = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        esterna.executeWithoutResult(stato -> {
            sottomissioni.salva(s);
            assertNull(s.getId());
            assertEquals(1, conta("sottomissione"));
            stato.setRollbackOnly();
        });
        assertNull(s.getId());
        assertEquals(0, conta("sottomissione"));
        sottomissioni.salva(s);
        Valutazione v = Valutazione.crea(s, giudice, datiValutazione());
        esterna.executeWithoutResult(stato -> {
            valutazioni.salva(v);
            assertNull(v.getId());
            assertEquals(1, conta("valutazione"));
            stato.setRollbackOnly();
        });
        assertNull(v.getId());
        assertEquals(0, conta("valutazione"));
        valutazioni.salva(v);
        assertNotNull(v.getId());
        assertEquals(1, conta("valutazione"));
    }

    @Test
    void assegnaIdAlCommitEsternoESalvaUnaSolaVolta() {
        Sottomissione s = Sottomissione.crea(creaPartecipazione(), "Soluzione");
        var esterna = new TransactionTemplate(new JdbcTransactionManager(dataSource));
        esterna.executeWithoutResult(stato -> {
            sottomissioni.salva(s);
            assertNull(s.getId());
            assertThrows(IllegalStateException.class, () -> sottomissioni.salva(s));
        });
        assertNotNull(s.getId());
        Valutazione v = Valutazione.crea(s, giudice, datiValutazione());
        esterna.executeWithoutResult(stato -> {
            valutazioni.salva(v);
            assertNull(v.getId());
            assertThrows(IllegalStateException.class, () -> valutazioni.salva(v));
        });
        assertNotNull(v.getId());
        assertEquals(1, conta("sottomissione"));
        assertEquals(1, conta("valutazione"));
    }

    @Test
    void conservaSottomissioneEValutazioneDopoLaRiapertura(@TempDir Path cartella) {
        String url = "jdbc:h2:file:" + cartella.resolve("hackhub").toAbsolutePath();
        var origine = new DriverManagerDataSource(url, "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(origine);
        collega(origine);
        creaAccountETeam();
        Partecipazione p = creaPartecipazione();
        Sottomissione s = salvaSottomissione(p);
        control(valutazioni).confermaValutazione(s, giudice, datiValutazione());
        jdbc.sql("SHUTDOWN").update();

        collega(new DriverManagerDataSource(url, "sa", ""));
        Sottomissione letta = sottomissioni.recuperaSottomissione(p);
        assertEquals(s.getId(), letta.getId());
        assertEquals(s.getContenuto(), letta.getContenuto());
        assertEquals(s.getValutazione().getId(), letta.getValutazione().getId());
        assertEquals(s.getValutazione().getDataOra(), letta.getValutazione().getDataOra());
        assertTrue(hackathons.ottieniHackathonValutabili(giudice).isEmpty());
        Sottomissione successiva = salvaSottomissione(creaPartecipazione());
        control(valutazioni).confermaValutazione(successiva, giudice, datiValutazione());
        assertTrue(successiva.getId() > s.getId());
        assertTrue(successiva.getValutazione().getId() > s.getValutazione().getId());
        jdbc.sql("SHUTDOWN").update();
    }

    private DataSource dataSourceConCommitFallito() {
        DataSource origine = dataSource;
        return new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                Connection reale = origine.getConnection();
                return (Connection) Proxy.newProxyInstance(
                        Connection.class.getClassLoader(), new Class<?>[]{Connection.class},
                        (proxy, metodo, argomenti) -> {
                            if (metodo.getName().equals("commit")) throw new SQLException("Commit fallito");
                            try { return metodo.invoke(reale, argomenti); }
                            catch (InvocationTargetException e) { throw e.getCause(); }
                        }
                );
            }

            @Override
            public Connection getConnection(String utente, String password) throws SQLException {
                return getConnection();
            }
        };
    }
}