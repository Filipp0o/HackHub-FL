package io.github.filipp0o.hackhub.infrastructure;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.datasource.embedded.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SchemaAssociazioniTest {

    private EmbeddedDatabase database;
    private JdbcClient jdbc;

    @BeforeEach
    void preparaSchemaPrecedenteConDati() throws Exception {
        database = new EmbeddedDatabaseBuilder()
                .generateUniqueName(true)
                .setType(EmbeddedDatabaseType.H2)
                .build();

        jdbc = JdbcClient.create(database);

        String schema;
        try (var input = new ClassPathResource("schema.sql").getInputStream()) {
            schema = new String(
                    input.readAllBytes(), StandardCharsets.UTF_8
            );
        }

        String precedente = schema.substring(
                0,
                schema.indexOf("-- Persistenza delle associazioni complete")
        );

        new ResourceDatabasePopulator(
                new ByteArrayResource(
                        precedente.getBytes(StandardCharsets.UTF_8)
                )
        ).execute(database);

        jdbc.sql("""
                INSERT INTO utente (id, email, password_hash)
                VALUES (1, 'uno@example.com', 'hash')
                """).update();

        jdbc.sql("""
                INSERT INTO team (id, nome, responsabile_id)
                VALUES (20, 'Team', 1)
                """).update();

        jdbc.sql("INSERT INTO team_membro VALUES (20, 1)").update();

        for (long id : new long[]{10L, 11L}) {
            jdbc.sql("""
                    INSERT INTO hackathon (
                        id, nome, regolamento, criteri_valutazione,
                        scadenza_iscrizioni, data_inizio, data_fine,
                        luogo, importo_premio, dimensione_massima_team,
                        tipo_stato, organizzatore_id, giudice_id
                    )
                    VALUES (
                        :id, 'Evento', 'Regole', 'Criteri',
                        '2025-04-01', '2025-04-02', '2025-04-03',
                        'Camerino', 10, 5, 'IN_VALUTAZIONE', 1, 1
                    )
                    """)
                    .param("id", id)
                    .update();
        }

        jdbc.sql("""
                INSERT INTO partecipazione (
                    id, hackathon_id, team_id, stato
                )
                VALUES (30, 10, 20, 'ATTIVA'), (31, 11, 20, 'ATTIVA')
                """).update();

        applicaSchema();
    }

    @AfterEach
    void chiudi() {
        if (database != null) {
            database.shutdown();
        }
    }

    private void applicaSchema() {
        new ResourceDatabasePopulator(
                new ClassPathResource("schema.sql")
        ).execute(database);
    }

    private void inserisciSottomissione() {
        jdbc.sql("""
                INSERT INTO sottomissione (id, partecipazione_id, contenuto)
                VALUES (40, 30, 'Progetto')
                """).update();
    }

    @Test
    void aggiornaSchemaPrecedenteERiesecuzioneConservaDati() {
        inserisciSottomissione();

        jdbc.sql("""
                UPDATE hackathon SET vincitrice_id = 30 WHERE id = 10
                """).update();

        applicaSchema();

        assertEquals(
                2L,
                jdbc.sql("SELECT COUNT(*) FROM hackathon")
                        .query(Long.class).single()
        );
        assertEquals(
                "Progetto",
                jdbc.sql("SELECT contenuto FROM sottomissione WHERE id = 40")
                        .query(String.class).single()
        );
        assertEquals(
                30L,
                jdbc.sql("SELECT vincitrice_id FROM hackathon WHERE id = 10")
                        .query(Long.class).single()
        );
    }

    @Test
    void conservaPunteggioTimestampERiferimentiPagamento() {
        inserisciSottomissione();

        BigDecimal punteggio = new BigDecimal("8.123456789");
        LocalDateTime data = LocalDateTime.of(
                2025, 4, 10, 12, 30, 1, 123456789
        );

        jdbc.sql("""
                INSERT INTO valutazione (
                    id, sottomissione_id, giudice_id,
                    giudizio, punteggio, data_ora
                )
                VALUES (50, 40, 1, 'Ottimo', :punteggio, :data)
                """)
                .param("punteggio", punteggio)
                .param("data", data)
                .update();

        assertEquals(
                0,
                punteggio.compareTo(
                        jdbc.sql("SELECT punteggio FROM valutazione WHERE id = 50")
                                .query(BigDecimal.class).single()
                )
        );
        assertEquals(
                data,
                jdbc.sql("SELECT data_ora FROM valutazione WHERE id = 50")
                        .query(LocalDateTime.class).single()
        );

        jdbc.sql("""
                INSERT INTO riscossione_premio (
                    hackathon_id, stato, beneficiary_ref, payment_ref
                )
                VALUES (10, 'EROGATA', 'beneficiario', 'pagamento')
                """).update();

        applicaSchema();

        assertEquals(
                "pagamento",
                jdbc.sql("""
                        SELECT payment_ref FROM riscossione_premio
                        WHERE hackathon_id = 10
                        """).query(String.class).single()
        );
    }

    @Test
    void vincoliUnivociImpedisconoAssociazioniDuplicate() {
        inserisciSottomissione();

        assertThrows(
                DataAccessException.class,
                () -> jdbc.sql("""
                        INSERT INTO sottomissione (partecipazione_id, contenuto)
                        VALUES (30, 'Duplicata')
                        """).update()
        );

        String valutazione = """
                INSERT INTO valutazione (
                    sottomissione_id, giudice_id, giudizio, punteggio, data_ora
                )
                VALUES (40, 1, 'Buono', 8, CURRENT_TIMESTAMP)
                """;

        jdbc.sql(valutazione).update();

        assertThrows(
                DataAccessException.class,
                () -> jdbc.sql(valutazione).update()
        );

        String riscossione = """
                INSERT INTO riscossione_premio (hackathon_id, stato)
                VALUES (10, 'DA_CONFIGURARE')
                """;

        jdbc.sql(riscossione).update();

        assertThrows(
                DataAccessException.class,
                () -> jdbc.sql(riscossione).update()
        );
    }

    @Test
    void vincitriceDeveEsistereEAppartenereAlloStessoHackathon() {
        assertThrows(
                DataAccessException.class,
                () -> jdbc.sql("""
                        UPDATE hackathon SET vincitrice_id = 999 WHERE id = 10
                        """).update()
        );
        assertThrows(
                DataAccessException.class,
                () -> jdbc.sql("""
                        UPDATE hackathon SET vincitrice_id = 31 WHERE id = 10
                        """).update()
        );

        jdbc.sql("""
                UPDATE hackathon SET vincitrice_id = 30 WHERE id = 10
                """).update();

        assertEquals(
                30L,
                jdbc.sql("SELECT vincitrice_id FROM hackathon WHERE id = 10")
                        .query(Long.class).single()
        );
    }

    @Test
    void rifiutaRiferimentiAssentiPunteggiEStatiIncoerenti() {
        assertThrows(
                DataAccessException.class,
                () -> jdbc.sql("""
                        INSERT INTO sottomissione (partecipazione_id, contenuto)
                        VALUES (999, 'Progetto')
                        """).update()
        );

        inserisciSottomissione();

        assertThrows(
                DataAccessException.class,
                () -> jdbc.sql("""
                        INSERT INTO valutazione (
                            sottomissione_id, giudice_id,
                            giudizio, punteggio, data_ora
                        )
                        VALUES (40, 1, 'Giudizio', 11, CURRENT_TIMESTAMP)
                        """).update()
        );

        for (String stato : new String[]{
                "PRONTA", "EROGATA", "SCONOSCIUTO"
        }) {
            assertThrows(
                    DataAccessException.class,
                    () -> jdbc.sql("""
                            INSERT INTO riscossione_premio (hackathon_id, stato)
                            VALUES (10, :stato)
                            """)
                            .param("stato", stato)
                            .update()
            );
        }

        assertEquals(
                0L,
                jdbc.sql("SELECT COUNT(*) FROM riscossione_premio")
                        .query(Long.class).single()
        );
    }
}