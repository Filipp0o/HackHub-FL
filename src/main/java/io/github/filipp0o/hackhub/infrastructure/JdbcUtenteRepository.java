package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.util.List;
import java.util.Objects;

public class JdbcUtenteRepository implements UtenteRepository {

    private final JdbcClient jdbcClient;

    public JdbcUtenteRepository(JdbcClient jdbcClient) {
        this.jdbcClient = Objects.requireNonNull(
                jdbcClient, "Il client JDBC è obbligatorio"
        );
    }

    @Override
    public List<Utente> recuperaUtentiAssegnabili() {
        try {
            return List.copyOf(jdbcClient.sql("""
                            SELECT id, email, password_hash
                            FROM utente
                            ORDER BY id
                            """)
                    .query((rs, rowNum) -> Utente.ricostruisci(
                            rs.getLong("id"),
                            rs.getString("email"),
                            rs.getString("password_hash")
                    ))
                    .list());
        } catch (DataAccessException eccezione) {
            throw new IllegalStateException(
                    "Impossibile recuperare gli utenti", eccezione
            );
        }
    }

    @Override
    public boolean esistePerEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email è obbligatoria");
        }

        try {
            return jdbcClient.sql("""
                            SELECT COUNT(*) FROM utente WHERE email = :email
                            """)
                    .param("email", email)
                    .query(Long.class)
                    .single() > 0;
        } catch (DataAccessException eccezione) {
            throw new IllegalStateException(
                    "Impossibile verificare l'email", eccezione
            );
        }
    }

    @Override
    public void salva(Utente utente) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");

        if (utente.recuperaEmail() == null || utente.recuperaEmail().isBlank()
                || utente.recuperaPasswordHash() == null
                || utente.recuperaPasswordHash().isBlank()) {
            throw new IllegalArgumentException(
                    "Il salvataggio richiede un account con email e hash"
            );
        }

        try {
            if (utente.getId() == null) {
                inserisci(utente);
            } else {
                aggiorna(utente);
            }
        } catch (DuplicateKeyException eccezione) {
            throw new IllegalStateException(
                    "L'email è già registrata", eccezione
            );
        } catch (DataAccessException eccezione) {
            throw new IllegalStateException(
                    "Impossibile salvare l'utente", eccezione
            );
        }
    }

    private void inserisci(Utente utente) {
        GeneratedKeyHolder chiave = new GeneratedKeyHolder();

        int righe = jdbcClient.sql("""
                        INSERT INTO utente (email, password_hash)
                        VALUES (:email, :passwordHash)
                        """)
                .param("email", utente.recuperaEmail())
                .param("passwordHash", utente.recuperaPasswordHash())
                .update(chiave, "ID");

        Number id = chiave.getKey();
        if (righe != 1 || id == null || id.longValue() <= 0) {
            throw new IllegalStateException(
                    "Inserimento utente senza identificativo valido"
            );
        }

        utente.assegnaId(id.longValue());
    }

    private void aggiorna(Utente utente) {
        int righe = jdbcClient.sql("""
                        UPDATE utente
                        SET email = :email, password_hash = :passwordHash
                        WHERE id = :id
                        """)
                .param("email", utente.recuperaEmail())
                .param("passwordHash", utente.recuperaPasswordHash())
                .param("id", utente.getId())
                .update();

        if (righe != 1) {
            throw new IllegalStateException(
                    "L'utente da aggiornare non esiste"
            );
        }
    }
}