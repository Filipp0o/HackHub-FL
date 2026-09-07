package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;

public class JdbcTeamRepository implements TeamRepository {

    private final JdbcClient jdbc;
    private final TransactionTemplate transazione;

    public JdbcTeamRepository(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "Il datasource è obbligatorio");
        jdbc = JdbcClient.create(dataSource);
        transazione = new TransactionTemplate(
                new JdbcTransactionManager(dataSource)
        );
    }

    @Override
    public boolean verificaAppartenenzaTeam(Utente utente) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");

        if (utente.getId() == null) {
            return false;
        }

        try {
            return jdbc.sql("""
                            SELECT COUNT(*)
                            FROM team_membro
                            WHERE utente_id = :id
                            """)
                    .param("id", utente.getId())
                    .query(Long.class)
                    .single() > 0;
        } catch (DataAccessException errore) {
            throw new IllegalStateException(
                    "Impossibile verificare l'appartenenza al team",
                    errore
            );
        }
    }

    @Override
    public Team recuperaTeam(Utente utente) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");

        if (utente.getId() == null) {
            throw new IllegalStateException(
                    "L'utente non appartiene ad alcun team"
            );
        }

        try {
            List<RigaTeam> righe = jdbc.sql("""
                            SELECT t.id, t.nome, t.responsabile_id,
                                   u.id AS membro_id, u.email, u.password_hash
                            FROM team t
                            JOIN team_membro m ON m.team_id = t.id
                            JOIN utente u ON u.id = m.utente_id
                            WHERE EXISTS (
                                SELECT 1 FROM team_membro appartenenza
                                WHERE appartenenza.team_id = t.id
                                  AND appartenenza.utente_id = :utenteId
                            )
                            ORDER BY u.id
                            """)
                    .param("utenteId", utente.getId())
                    .query((rs, numero) -> new RigaTeam(
                            rs.getLong("id"),
                            rs.getString("nome"),
                            rs.getLong("responsabile_id"),
                            Utente.ricostruisci(
                                    rs.getLong("membro_id"),
                                    rs.getString("email"),
                                    rs.getString("password_hash")
                            )
                    ))
                    .list();

            if (righe.isEmpty()) {
                throw new IllegalStateException(
                        "L'utente non appartiene ad alcun team"
                );
            }

            RigaTeam prima = righe.get(0);
            List<Utente> membri = righe.stream()
                    .map(RigaTeam::membro)
                    .toList();

            Utente responsabile = membri.stream()
                    .filter(membro ->
                            prima.responsabileId().equals(membro.getId()))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException(
                            "Responsabile assente dai membri"
                    ));

            return Team.ricostruisci(
                    prima.id(), prima.nome(), membri, responsabile
            );
        } catch (DataAccessException errore) {
            throw new IllegalStateException(
                    "Impossibile recuperare il team", errore
            );
        }
    }

    @Override
    public void salva(Team team) {
        Objects.requireNonNull(team, "Il team è obbligatorio");

        List<Utente> membri = team.getMembri();

        if (membri.stream().anyMatch(membro -> membro.getId() == null)
                || team.getResponsabile().getId() == null) {
            throw new IllegalArgumentException(
                    "Gli utenti del team devono essere già salvati"
            );
        }

        boolean nuovo = team.getId() == null;

        try {
            Long id = transazione.execute(stato -> {
                Long teamId = nuovo ? inserisci(team) : aggiorna(team);

                jdbc.sql("DELETE FROM team_membro WHERE team_id = :id")
                        .param("id", teamId)
                        .update();

                for (Utente membro : membri) {
                    jdbc.sql("""
                                    INSERT INTO team_membro (team_id, utente_id)
                                    VALUES (:teamId, :utenteId)
                                    """)
                            .param("teamId", teamId)
                            .param("utenteId", membro.getId())
                            .update();
                }

                return teamId;
            });

            if (nuovo) {
                team.assegnaId(Objects.requireNonNull(id));
            }
        } catch (DataAccessException errore) {
            throw new IllegalStateException(
                    "Impossibile salvare il team", errore
            );
        }
    }

    private Long inserisci(Team team) {
        GeneratedKeyHolder chiave = new GeneratedKeyHolder();

        int righe = jdbc.sql("""
                        INSERT INTO team (nome, responsabile_id)
                        VALUES (:nome, :responsabileId)
                        """)
                .param("nome", team.getNome())
                .param("responsabileId", team.getResponsabile().getId())
                .update(chiave, "ID");

        Number id = chiave.getKey();

        if (righe != 1 || id == null || id.longValue() <= 0) {
            throw new IllegalStateException(
                    "Inserimento team senza identificativo valido"
            );
        }

        return id.longValue();
    }

    private Long aggiorna(Team team) {
        int righe = jdbc.sql("""
                        UPDATE team
                        SET nome = :nome, responsabile_id = :responsabileId
                        WHERE id = :id
                        """)
                .param("nome", team.getNome())
                .param("responsabileId", team.getResponsabile().getId())
                .param("id", team.getId())
                .update();

        if (righe != 1) {
            throw new IllegalStateException(
                    "Il team da aggiornare non esiste"
            );
        }

        return team.getId();
    }

    private record RigaTeam(
            Long id,
            String nome,
            Long responsabileId,
            Utente membro
    ) { }
}