package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.domain.Invito;
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

class JdbcInvitoRepositoryTest {

    private EmbeddedDatabase database;
    private JdbcClient jdbc;
    private JdbcInvitoRepository inviti;
    private JdbcTeamRepository teams;
    private Utente creatore;
    private Utente membro;
    private Utente destinatario;
    private Team team;

    @BeforeEach
    void prepara() {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .addScript("schema.sql")
                .build();

        jdbc = JdbcClient.create(database);
        inviti = new JdbcInvitoRepository(jdbc);
        teams = new JdbcTeamRepository(database);

        var utenti = new JdbcUtenteRepository(jdbc);

        creatore = Utente.crea(
                "creatore@example.com", "hash-creatore"
        );
        membro = Utente.crea(
                "membro@example.com", "hash-membro"
        );
        destinatario = Utente.crea(
                "destinatario@example.com", "hash-destinatario"
        );

        utenti.salva(creatore);
        utenti.salva(membro);
        utenti.salva(destinatario);

        team = Team.crea("ByteBuilders", creatore, creatore);
        team.aggiungiMembro(membro);
        teams.salva(team);
    }

    @AfterEach
    void chiudi() {
        if (database != null) {
            database.shutdown();
        }
    }

    private Invito crea() {
        Invito invito = Invito.crea(team, destinatario);
        inviti.salva(invito);
        return invito;
    }

    private long numeroInviti() {
        return jdbc.sql("SELECT COUNT(*) FROM invito")
                .query(Long.class)
                .single();
    }

    @Test
    void ricostruisceInvitoTeamEMembriCompletiDaNuovaIstanza() {
        Invito originale = crea();

        var ricevuti = new JdbcInvitoRepository(JdbcClient.create(database))
                .recuperaInvitiRicevuti(new Utente(destinatario.getId()));

        assertEquals(1, ricevuti.size());

        Invito letto = ricevuti.get(0);

        assertNotSame(originale, letto);
        assertEquals(originale.getId(), letto.getId());
        assertFalse(letto.isAccettato());
        assertEquals(
                "hash-destinatario",
                letto.getDestinatario().recuperaPasswordHash()
        );
        assertEquals(
                "destinatario@example.com",
                letto.getDestinatario().recuperaEmail()
        );
        assertEquals(team.getId(), letto.ottieniTeam().getId());
        assertEquals("ByteBuilders", letto.ottieniTeam().getNome());
        assertEquals(
                List.of(creatore.getId(), membro.getId()),
                letto.ottieniTeam().getMembri().stream()
                        .map(Utente::getId).toList()
        );
        assertEquals(
                "hash-membro",
                letto.ottieniTeam().getMembri().get(1).recuperaPasswordHash()
        );
        assertEquals(
                creatore.getId(),
                letto.ottieniTeam().getResponsabile().getId()
        );
        assertThrows(UnsupportedOperationException.class, ricevuti::clear);
    }

    @Test
    void filtraDestinatarioEStatoEOrdinaSenzaDuplicarePerMembro() {
        Invito primo = crea();

        Invito accettato = crea();
        accettato.registraAccettazione();
        inviti.salva(accettato);

        inviti.salva(Invito.crea(team, creatore));
        Invito ultimo = crea();

        assertEquals(
                List.of(primo.getId(), ultimo.getId()),
                inviti.recuperaInvitiRicevuti(destinatario).stream()
                        .map(Invito::getId).toList()
        );
        assertEquals(1, inviti.recuperaInvitiRicevuti(creatore).size());
        assertTrue(inviti.recuperaInvitiRicevuti(membro).isEmpty());
    }

    @Test
    void salvaDueVolteEAggiornaStatoSenzaDuplicare() {
        Invito invito = crea();
        inviti.salva(invito);

        assertEquals(1, numeroInviti());

        Invito letto = inviti.recuperaInvitiRicevuti(destinatario).get(0);
        letto.registraAccettazione();
        inviti.salva(letto);

        assertEquals(1, numeroInviti());
        assertTrue(inviti.recuperaInvitiRicevuti(destinatario).isEmpty());
        assertTrue(
                jdbc.sql("SELECT accettato FROM invito WHERE id = :id")
                        .param("id", invito.getId())
                        .query(Boolean.class).single()
        );
    }

    @Test
    void nuovaIstanzaUsaIdentificativiGeneratiDalDatabase() {
        Invito primo = crea();
        Invito secondo = Invito.crea(team, destinatario);

        new JdbcInvitoRepository(JdbcClient.create(database))
                .salva(secondo);

        assertNotNull(primo.getId());
        assertNotEquals(primo.getId(), secondo.getId());
        assertEquals(2, numeroInviti());
    }

    @Test
    void rifiutaAggiornamentoAssenteODiAssociazioniDiverse() {
        Invito originale = crea();
        Team altro = Team.crea("Altro", destinatario, destinatario);
        teams.salva(altro);

        for (Invito errato : new Invito[]{
                Invito.ricostruisci(999L, team, destinatario, true),
                Invito.ricostruisci(
                        originale.getId(), team, creatore, true
                ),
                Invito.ricostruisci(
                        originale.getId(), altro, destinatario, true
                )
        }) {
            assertThrows(
                    IllegalStateException.class,
                    () -> inviti.salva(errato)
            );
        }

        assertEquals(1, numeroInviti());
        assertEquals(
                originale.getId(),
                inviti.recuperaInvitiRicevuti(destinatario).get(0).getId()
        );
    }

    @Test
    void riferimentiInesistentiNonInserisconoRigheENonAssegnanoId() {
        Team assente = Team.ricostruisci(
                999L, "Assente", List.of(creatore), creatore
        );

        for (Invito errato : new Invito[]{
                Invito.crea(team, new Utente(999L)),
                Invito.crea(assente, destinatario)
        }) {
            assertThrows(
                    IllegalStateException.class,
                    () -> inviti.salva(errato)
            );
            assertNull(errato.getId());
        }

        assertEquals(0, numeroInviti());
    }

    @Test
    void transazioneEsternaAnnullaAccettazioneEAggiuntaDelMembro() {
        Invito originale = crea();
        var transazione = new TransactionTemplate(
                new JdbcTransactionManager(database)
        );

        assertThrows(
                IllegalStateException.class,
                () -> transazione.executeWithoutResult(stato -> {
                    Invito letto = inviti
                            .recuperaInvitiRicevuti(destinatario).get(0);

                    letto.registraAccettazione();
                    letto.ottieniTeam().aggiungiMembro(destinatario);

                    inviti.salva(letto);
                    teams.salva(letto.ottieniTeam());

                    throw new IllegalStateException(
                            "Errore dopo entrambe le scritture"
                    );
                })
        );

        assertEquals(
                originale.getId(),
                inviti.recuperaInvitiRicevuti(destinatario).get(0).getId()
        );
        assertEquals(2, teams.recuperaTeam(creatore).numeroMembri());
        assertFalse(teams.verificaAppartenenzaTeam(destinatario));
    }

    @Test
    void commitEsternoConservaAccettazioneEMembro() {
        crea();

        new TransactionTemplate(new JdbcTransactionManager(database))
                .executeWithoutResult(stato -> {
                    Invito letto = inviti
                            .recuperaInvitiRicevuti(destinatario).get(0);

                    letto.registraAccettazione();
                    letto.ottieniTeam().aggiungiMembro(destinatario);

                    inviti.salva(letto);
                    teams.salva(letto.ottieniTeam());
                });

        assertTrue(inviti.recuperaInvitiRicevuti(destinatario).isEmpty());
        assertEquals(
                team.getId(),
                teams.recuperaTeam(destinatario).getId()
        );
        assertEquals(3, teams.recuperaTeam(creatore).numeroMembri());
    }

    @Test
    void rifiutaNullERiferimentiNonSalvati() {
        assertThrows(
                NullPointerException.class,
                () -> new JdbcInvitoRepository(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> inviti.salva(null)
        );
        assertThrows(
                NullPointerException.class,
                () -> inviti.recuperaInvitiRicevuti(null)
        );

        Utente nuovo = Utente.crea("nuovo@example.com", "hash");

        assertTrue(inviti.recuperaInvitiRicevuti(nuovo).isEmpty());
        assertThrows(
                IllegalArgumentException.class,
                () -> inviti.salva(Invito.crea(team, nuovo))
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> inviti.salva(Invito.crea(
                        Team.crea("Nuovo", creatore, creatore),
                        destinatario
                ))
        );

        assertEquals(0, numeroInviti());
    }
}