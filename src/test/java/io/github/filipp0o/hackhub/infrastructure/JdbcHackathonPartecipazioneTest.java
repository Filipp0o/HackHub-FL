package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.ProclamareTeamVincitoreControl;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class JdbcHackathonPartecipazioneTest {

    private EmbeddedDatabase database;
    private JdbcClient jdbc;
    private JdbcHackathonRepository hackathons;
    private JdbcPartecipazioneRepository partecipazioni;
    private DataSource dataSourceCorrente;
    private Utente organizzatore, giudice, mentore, responsabile, membro;
    private Team team;
    private static final LocalDateTime DATA_VALUTAZIONE =
            LocalDateTime.of(2026, 1, 2, 12, 30, 1, 123456789);

    @BeforeEach
    void prepara() {
        database = new EmbeddedDatabaseBuilder().generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2).addScript("schema.sql").build();
        collega(database);
        creaAccountETeam();
    }

    private void collega(DataSource origine) {
        dataSourceCorrente = origine;
        jdbc = JdbcClient.create(origine);
        hackathons = new JdbcHackathonRepository(origine);
        partecipazioni = new JdbcPartecipazioneRepository(origine, hackathons);
    }

    private void creaAccountETeam() {
        var utenti = new JdbcUtenteRepository(jdbc);
        organizzatore = Utente.crea("organizzatore@example.com", "hash-organizzatore");
        giudice = Utente.crea("giudice@example.com", "hash-giudice");
        mentore = Utente.crea("mentore@example.com", "hash-mentore");
        responsabile = Utente.crea("responsabile@example.com", "hash-responsabile");
        membro = Utente.crea("membro@example.com", "hash-membro");
        List.of(organizzatore, giudice, mentore, responsabile, membro).forEach(utenti::salva);
        team = Team.crea("ByteBuilders", responsabile, responsabile);
        team.aggiungiMembro(membro);
        new JdbcTeamRepository(dataSourceCorrente).salva(team);
    }

    @AfterEach
    void chiudi() {
        if (database != null) database.shutdown();
    }

    private DatiHackathon dati(boolean passato, String premio) {
        LocalDate inizio = LocalDate.now().plusDays(passato ? -10 : 10);
        return new DatiHackathon(
                "HackHub", "Regolamento", "Criteri", inizio.minusDays(2),
                inizio, inizio.plusDays(2), "Camerino", new BigDecimal(premio), 5
        );
    }

    private Hackathon crea(boolean passato) {
        Hackathon h = Hackathon.crea(
                dati(passato, "100.50"), organizzatore, giudice, List.of(mentore)
        );
        hackathons.salva(h);
        return h;
    }

    private Partecipazione iscrivi(Hackathon h) {
        Partecipazione p = Partecipazione.crea(h, team);
        partecipazioni.salva(p);
        return p;
    }

    private void inserisciSottomissione(Partecipazione p, boolean valutata) {
        jdbc.sql("INSERT INTO sottomissione (partecipazione_id, contenuto) VALUES (:id, :contenuto)")
                .param("id", p.getId()).param("contenuto", "Soluzione completa").update();
        if (valutata) {
            jdbc.sql("""
                    INSERT INTO valutazione (sottomissione_id, giudice_id, giudizio, punteggio, data_ora)
                    SELECT id, :giudice, 'Ottimo lavoro', 8.75, :data FROM sottomissione WHERE partecipazione_id = :id
                    """).param("giudice", giudice.getId()).param("data", DATA_VALUTAZIONE)
                    .param("id", p.getId()).update();
        }
    }

    private ProclamareTeamVincitoreControl proclamazione() {
        return new ProclamareTeamVincitoreControl(
                partecipazioni, hackathons, new SegnalazioneRepositoryImpl()
        );
    }

    private long conta(String tabella) {
        return jdbc.sql("SELECT COUNT(*) FROM " + tabella).query(Long.class).single();
    }

    @Test
    void salvaAggiornaERicostruisceStaffCompletoSenzaDuplicazioni() {
        Hackathon h = crea(false);
        hackathons.salva(h);
        Hackathon letto = new JdbcHackathonRepository(dataSourceCorrente)
                .recuperaHackathon(h.getId());
        assertAll(
                () -> assertNotSame(h, letto),
                () -> assertEquals(h.getId(), letto.getId()),
                () -> assertEquals("100.50", letto.getImportoPremio().toPlainString()),
                () -> assertEquals(
                        "organizzatore@example.com", letto.getOrganizzatore().recuperaEmail()
                ),
                () -> assertEquals("hash-giudice", letto.getGiudice().recuperaPasswordHash()),
                () -> assertEquals(
                        List.of(mentore.getId()),
                        letto.getMentori().stream().map(Utente::getId).toList()
                ),
                () -> assertEquals(TipoStatoHackathon.IN_ISCRIZIONE, letto.getStato()),
                () -> assertEquals(1, conta("hackathon")),
                () -> assertEquals(1, conta("hackathon_mentore"))
        );
        Hackathon aggiornato = Hackathon.ricostruisci(new DatiRipristinoHackathon(
                h.getId(), dati(false, "200.00"), organizzatore, mentore,
                List.of(giudice, responsabile), h.getStato(), null, null
        ));
        hackathons.salva(aggiornato);
        Hackathon riletto = hackathons.recuperaHackathon(h.getId());
        assertEquals(new BigDecimal("200.00"), riletto.getImportoPremio());
        assertEquals(mentore.getId(), riletto.getGiudice().getId());
        assertEquals(2, riletto.getMentori().size());
    }

    @Test
    void elenchiAggiornanoEPersistonoLoStatoPrimaDeiFiltri() {
        Hackathon scaduto = crea(true);
        inserisciSottomissione(iscrivi(scaduto), false);
        Hackathon futuro = crea(false);
        assertEquals(
                List.of(scaduto.getId()),
                hackathons.ottieniHackathonValutabili(giudice).stream().map(Hackathon::getId).toList()
        );
        assertEquals(
                List.of(scaduto.getId()),
                hackathons.ottieniHackathonSegnalabili(mentore).stream().map(Hackathon::getId).toList()
        );
        assertEquals(
                List.of(futuro.getId()),
                hackathons.ottieniHackathonApertiAlleIscrizioni().stream().map(Hackathon::getId).toList()
        );
        assertTrue(hackathons.ottieniHackathonValutabili(mentore).isEmpty());
        assertTrue(hackathons.ottieniHackathonSegnalabili(giudice).isEmpty());
        assertEquals(
                "IN_VALUTAZIONE",
                jdbc.sql("SELECT tipo_stato FROM hackathon WHERE id = :id")
                        .param("id", scaduto.getId()).query(String.class).single()
        );
        assertEquals(2, hackathons.ottieniTuttiHackathon().size());
    }

    @Test
    void partecipazioniMantengonoTeamSottomissioneValutazioneEIdentitaDelGrafo() {
        Hackathon h = crea(true);
        Partecipazione p = iscrivi(h);
        inserisciSottomissione(p, true);
        Partecipazione letta = partecipazioni.recuperaPartecipazione(team, h);
        assertEquals(p.getId(), letta.getId());
        assertEquals(2, letta.getTeam().numeroMembri());
        assertEquals("hash-membro", letta.getTeam().getMembri().get(1).recuperaPasswordHash());
        assertSame(letta, letta.getSottomissione().getPartecipazione());
        assertSame(
                letta.getSottomissione(),
                letta.getSottomissione().getValutazione().getSottomissione()
        );
        assertEquals(
                new BigDecimal("8.75"),
                letta.getSottomissione().getValutazione().getPunteggio()
        );
        assertEquals(DATA_VALUTAZIONE, letta.getSottomissione().getValutazione().getDataOra());
        assertTrue(hackathons.ottieniHackathonValutabili(giudice).isEmpty());
        letta.escludi();
        partecipazioni.salva(letta);
        partecipazioni.salva(letta);
        assertEquals(1, conta("partecipazione"));
        assertEquals(1, conta("sottomissione"));
        assertEquals(1, conta("valutazione"));
        assertTrue(partecipazioni.recuperaPartecipazioniNonEscluse(h).isEmpty());
        assertNotNull(
                partecipazioni.recuperaPartecipazione(team, h).getSottomissione().getValutazione()
        );
    }

    @Test
    void proclamazioneERiscossioneSiRicaricanoComplete() {
        Hackathon h = crea(true);
        inserisciSottomissione(iscrivi(h), true);
        Partecipazione p = partecipazioni.recuperaPartecipazione(team, h);
        proclamazione().confermaProclamazione(organizzatore, h, p);
        assertNotNull(h.getRiscossionePremio().getId());
        h.getRiscossionePremio().configura("beneficiario-1");
        hackathons.salva(h);
        h = hackathons.recuperaHackathon(h.getId());
        h.getRiscossionePremio().registraErogazione("pagamento-1");
        hackathons.salva(h);
        Hackathon letto = hackathons.recuperaHackathon(h.getId());
        assertEquals(TipoStatoHackathon.CONCLUSO, letto.getStato());
        assertSame(letto, letto.getVincitrice().getHackathon());
        assertSame(letto, letto.getRiscossionePremio().getHackathon());
        assertEquals("pagamento-1", letto.getRiscossionePremio().getPaymentRef());
        assertEquals(StatoRiscossionePremio.EROGATA, letto.getRiscossionePremio().getStato());
        assertNotNull(letto.getVincitrice().getSottomissione().getValutazione());
        Partecipazione vincitrice = partecipazioni.ottieniPartecipazioni(letto).getFirst();
        assertSame(vincitrice, vincitrice.getHackathon().getVincitrice());
        assertTrue(partecipazioni.recuperaPartecipazioniInHackathonNonConclusi(team).isEmpty());
        assertEquals(1, conta("riscossione_premio"));
    }

    @Test
    void erroreStaffAnnullaInserimentoEAggiornamento() {
        Hackathon nuovo = Hackathon.crea(
                dati(false, "100"), organizzatore, giudice, List.of(new Utente(999L))
        );
        assertThrows(IllegalStateException.class, () -> hackathons.salva(nuovo));
        assertNull(nuovo.getId());
        assertEquals(0, conta("hackathon"));
        Hackathon precedente = crea(false);
        Hackathon modificato = Hackathon.ricostruisci(new DatiRipristinoHackathon(
                precedente.getId(), dati(false, "500"), organizzatore, giudice,
                List.of(new Utente(999L)), precedente.getStato(), null, null
        ));
        assertThrows(IllegalStateException.class, () -> hackathons.salva(modificato));
        Hackathon letto = hackathons.recuperaHackathon(precedente.getId());
        assertEquals(new BigDecimal("100.50"), letto.getImportoPremio());
        assertEquals(mentore.getId(), letto.getMentori().getFirst().getId());
    }

    @Test
    void erroreUltimaScritturaRipristinaProclamazioneEConsenteNuovoTentativo() {
        Hackathon h = crea(true);
        inserisciSottomissione(iscrivi(h), true);
        Partecipazione p = partecipazioni.recuperaPartecipazione(team, h);
        jdbc.sql("""
                ALTER TABLE riscossione_premio
                ADD CONSTRAINT prova_errore CHECK (stato <> 'DA_CONFIGURARE')
                """).update();
        assertThrows(
                IllegalStateException.class,
                () -> proclamazione().confermaProclamazione(organizzatore, h, p)
        );
        assertNull(h.getVincitrice());
        assertNull(h.getRiscossionePremio());
        assertEquals(TipoStatoHackathon.IN_VALUTAZIONE, h.getStato());
        Hackathon letto = hackathons.recuperaHackathon(h.getId());
        assertNull(letto.getVincitrice());
        assertNull(letto.getRiscossionePremio());
        assertEquals(0, conta("riscossione_premio"));
        jdbc.sql("ALTER TABLE riscossione_premio DROP CONSTRAINT prova_errore").update();
        assertDoesNotThrow(
                () -> proclamazione().confermaProclamazione(organizzatore, h, p)
        );
        assertNotNull(hackathons.recuperaHackathon(h.getId()).getRiscossionePremio());
    }

    @Test
    void rifiutaDuplicatiAssociazioniErrateEUpdateDiRecordAssenti() {
        Hackathon h = crea(false);
        Partecipazione p = iscrivi(h);
        Partecipazione duplicata = Partecipazione.crea(h, team);
        assertThrows(IllegalStateException.class, () -> partecipazioni.salva(duplicata));
        assertNull(duplicata.getId());
        Hackathon altro = crea(false);
        Partecipazione errata = Partecipazione.ricostruisci(
                p.getId(), altro, team, StatoPartecipazione.ESCLUSA
        );
        assertThrows(IllegalStateException.class, () -> partecipazioni.salva(errata));
        assertEquals(
                StatoPartecipazione.ATTIVA,
                partecipazioni.recuperaPartecipazione(team, h).getStato()
        );
        assertThrows(
                IllegalStateException.class,
                () -> partecipazioni.salva(
                        Partecipazione.ricostruisci(999L, h, team, StatoPartecipazione.ATTIVA)
                )
        );
        Hackathon assente = Hackathon.ricostruisci(new DatiRipristinoHackathon(
                999L, dati(false, "100"), organizzatore, giudice, List.of(mentore),
                TipoStatoHackathon.IN_ISCRIZIONE, null, null
        ));
        assertThrows(IllegalStateException.class, () -> hackathons.salva(assente));
        assertTrue(partecipazioni.esistePartecipazione(team, h));
        assertFalse(partecipazioni.esistePartecipazione(team, altro));
        assertEquals(1, partecipazioni.recuperaPartecipazioniInHackathonNonConclusi(team).size());
    }

    @Test
    void rollbackEsternoNonAssegnaIdEAnnullaModifiche() {
        Hackathon h = crea(false);
        Partecipazione p = Partecipazione.crea(h, team);
        var esterna = new TransactionTemplate(new JdbcTransactionManager(dataSourceCorrente));
        esterna.executeWithoutResult(stato -> {
            partecipazioni.salva(p);
            assertNull(p.getId());
            assertEquals(1, conta("partecipazione"));
            stato.setRollbackOnly();
        });
        assertNull(p.getId());
        assertEquals(0, conta("partecipazione"));
        partecipazioni.salva(p);
        esterna.executeWithoutResult(stato -> {
            p.escludi();
            partecipazioni.salva(p);
            stato.setRollbackOnly();
        });
        assertEquals(
                StatoPartecipazione.ATTIVA,
                partecipazioni.recuperaPartecipazione(team, h).getStato()
        );
    }

    @Test
    void assegnaIdAlCommitEsternoERifiutaReinserimentiPrimaDelCommit() {
        var esterna = new TransactionTemplate(new JdbcTransactionManager(dataSourceCorrente));
        Hackathon h = Hackathon.crea(
                dati(false, "100"), organizzatore, giudice, List.of(mentore)
        );
        esterna.executeWithoutResult(stato -> {
            hackathons.salva(h);
            assertNull(h.getId());
            assertThrows(IllegalStateException.class, () -> hackathons.salva(h));
        });
        assertNotNull(h.getId());
        assertEquals(1, conta("hackathon"));
        Partecipazione p = Partecipazione.crea(h, team);
        esterna.executeWithoutResult(stato -> {
            partecipazioni.salva(p);
            assertNull(p.getId());
            assertThrows(IllegalStateException.class, () -> partecipazioni.salva(p));
        });
        assertNotNull(p.getId());
        assertEquals(1, conta("partecipazione"));
    }

    @Test
    void unOggettoIncompletoNonCancellaVincitriceERiscossioneGiaSalvate() {
        Hackathon h = crea(true);
        inserisciSottomissione(iscrivi(h), true);
        proclamazione().confermaProclamazione(
                organizzatore, h, partecipazioni.recuperaPartecipazione(team, h)
        );
        Hackathon incompleto = Hackathon.ricostruisci(new DatiRipristinoHackathon(
                h.getId(), dati(true, "100.50"), organizzatore, giudice, List.of(mentore),
                TipoStatoHackathon.IN_VALUTAZIONE, null, null
        ));
        assertThrows(IllegalStateException.class, () -> hackathons.salva(incompleto));
        Hackathon letto = hackathons.recuperaHackathon(h.getId());
        assertEquals(TipoStatoHackathon.CONCLUSO, letto.getStato());
        assertNotNull(letto.getVincitrice());
        assertNotNull(letto.getRiscossionePremio());
    }

    @Test
    void erroreAlCommitNonAssegnaIdENonLasciaRighe() {
        Hackathon h = Hackathon.crea(
                dati(false, "100"), organizzatore, giudice, List.of(mentore)
        );
        DataSource guasto = new AbstractDataSource() {
            @Override
            public Connection getConnection() throws SQLException {
                Connection reale = dataSourceCorrente.getConnection();
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
            public Connection getConnection(String utente, String password) throws SQLException {
                return getConnection();
            }
        };
        assertThrows(
                IllegalStateException.class,
                () -> new JdbcHackathonRepository(guasto).salva(h)
        );
        assertNull(h.getId());
        assertEquals(0, conta("hackathon"));
        assertEquals(0, conta("hackathon_mentore"));
        hackathons.salva(h);
        Partecipazione p = Partecipazione.crea(h, team);
        assertThrows(
                IllegalStateException.class,
                () -> new JdbcPartecipazioneRepository(guasto, hackathons).salva(p)
        );
        assertNull(p.getId());
        assertEquals(0, conta("partecipazione"));

        Hackathon passato = crea(true);
        inserisciSottomissione(iscrivi(passato), true);
        Partecipazione selezionata = partecipazioni.recuperaPartecipazione(team, passato);
        var control = new ProclamareTeamVincitoreControl(
                partecipazioni, new JdbcHackathonRepository(guasto), new SegnalazioneRepositoryImpl()
        );
        assertThrows(
                IllegalStateException.class,
                () -> control.confermaProclamazione(organizzatore, passato, selezionata)
        );
        assertEquals(TipoStatoHackathon.IN_VALUTAZIONE, passato.getStato());
        assertNull(passato.getVincitrice());
        assertNull(passato.getRiscossionePremio());
        assertNull(hackathons.recuperaHackathon(passato.getId()).getVincitrice());
        assertEquals(0, conta("riscossione_premio"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"0.001", "1.234", "100000000000000000.00"})
    void impedisceArrotondamentiOPerditaDiPrecisioneDelPremio(String premio) {
        Hackathon h = Hackathon.crea(
                dati(false, premio), organizzatore, giudice, List.of(mentore)
        );
        assertThrows(IllegalArgumentException.class, () -> hackathons.salva(h));
        assertNull(h.getId());
        assertEquals(0, conta("hackathon"));
    }

    @Test
    void datiESuccessiviIdSopravvivonoAllaChiusuraDelDatabase(@TempDir Path cartella) {
        String url = "jdbc:h2:file:" + cartella.resolve("hackhub").toAbsolutePath();
        var prima = new DriverManagerDataSource(url, "sa", "");
        new ResourceDatabasePopulator(new ClassPathResource("schema.sql")).execute(prima);
        collega(prima);
        creaAccountETeam();
        Hackathon h = crea(true);
        inserisciSottomissione(iscrivi(h), true);
        proclamazione().confermaProclamazione(
                organizzatore, h, partecipazioni.recuperaPartecipazione(team, h)
        );
        Long id = h.getId();
        jdbc.sql("SHUTDOWN").update();
        collega(new DriverManagerDataSource(url, "sa", ""));
        Hackathon letto = hackathons.recuperaHackathon(id);
        assertEquals(TipoStatoHackathon.CONCLUSO, letto.getStato());
        assertNotNull(letto.getVincitrice().getSottomissione().getValutazione());
        assertNotNull(letto.getRiscossionePremio().getId());
        assertEquals(
                2, partecipazioni.ottieniPartecipazioni(letto).getFirst().getTeam().numeroMembri()
        );
        assertTrue(crea(false).getId() > id);
        jdbc.sql("SHUTDOWN").update();
    }
}