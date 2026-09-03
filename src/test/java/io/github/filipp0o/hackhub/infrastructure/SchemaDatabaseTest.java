package io.github.filipp0o.hackhub.infrastructure;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class SchemaDatabaseTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    void creaLoSchemaMinimoDellaPersistenza() {
        List<String> tabelleCreate = jdbcClient.sql("""
                        SELECT TABLE_NAME
                        FROM INFORMATION_SCHEMA.TABLES
                        WHERE TABLE_SCHEMA = 'PUBLIC'
                        """)
                .query(String.class)
                .list();

        assertTrue(
                Set.copyOf(tabelleCreate).containsAll(
                        Set.of(
                                "UTENTE",
                                "TEAM",
                                "TEAM_MEMBRO",
                                "INVITO",
                                "HACKATHON",
                                "HACKATHON_MENTORE",
                                "PARTECIPAZIONE"
                        )
                )
        );
    }
}