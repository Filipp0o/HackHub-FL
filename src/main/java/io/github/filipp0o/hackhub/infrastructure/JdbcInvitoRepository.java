package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.InvitoRepository;
import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class JdbcInvitoRepository implements InvitoRepository {

    private final JdbcClient jdbc;

    public JdbcInvitoRepository(JdbcClient jdbc) {
        this.jdbc = Objects.requireNonNull(
                jdbc, "Il client JDBC è obbligatorio"
        );
    }

    @Override
    public List<Invito> recuperaInvitiRicevuti(Utente utente) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");

        if (utente.getId() == null) {
            return List.of();
        }

        try {
            List<RigaInvito> righe = jdbc.sql("""
                            SELECT i.id, t.id AS team_id, t.nome, t.responsabile_id,
                                   d.id AS destinatario_id,
                                   d.email AS destinatario_email,
                                   d.password_hash AS destinatario_hash,
                                   u.id AS membro_id,
                                   u.email AS membro_email,
                                   u.password_hash AS membro_hash
                            FROM invito i
                            JOIN team t ON t.id = i.team_id
                            JOIN utente d ON d.id = i.destinatario_id
                            JOIN team_membro m ON m.team_id = t.id
                            JOIN utente u ON u.id = m.utente_id
                            WHERE i.destinatario_id = :id
                              AND i.accettato = FALSE
                            ORDER BY i.id, u.id
                            """)
                    .param("id", utente.getId())
                    .query((rs, numero) -> new RigaInvito(
                            rs.getLong("id"),
                            rs.getLong("team_id"),
                            rs.getString("nome"),
                            rs.getLong("responsabile_id"),
                            Utente.ricostruisci(
                                    rs.getLong("destinatario_id"),
                                    rs.getString("destinatario_email"),
                                    rs.getString("destinatario_hash")
                            ),
                            Utente.ricostruisci(
                                    rs.getLong("membro_id"),
                                    rs.getString("membro_email"),
                                    rs.getString("membro_hash")
                            )
                    ))
                    .list();

            return righe.stream()
                    .collect(Collectors.groupingBy(
                            RigaInvito::id,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ))
                    .values().stream()
                    .map(this::ricostruisci)
                    .toList();
        } catch (DataAccessException errore) {
            throw new IllegalStateException(
                    "Impossibile recuperare gli inviti", errore
            );
        }
    }

    private Invito ricostruisci(List<RigaInvito> righe) {
        RigaInvito prima = righe.get(0);

        List<Utente> membri = righe.stream()
                .map(RigaInvito::membro)
                .toList();

        Utente responsabile = membri.stream()
                .filter(membro ->
                        prima.responsabileId().equals(membro.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Responsabile assente dai membri"
                ));

        Team team = Team.ricostruisci(
                prima.teamId(), prima.nome(), membri, responsabile
        );

        return Invito.ricostruisci(
                prima.id(), team, prima.destinatario(), false
        );
    }

    @Override
    public void salva(Invito invito) {
        Objects.requireNonNull(invito, "L'invito è obbligatorio");

        if (invito.ottieniTeam().getId() == null
                || invito.getDestinatario().getId() == null) {
            throw new IllegalArgumentException(
                    "Team e destinatario devono essere già salvati"
            );
        }

        try {
            if (invito.getId() == null) {
                inserisci(invito);
            } else {
                aggiorna(invito);
            }
        } catch (DataAccessException errore) {
            throw new IllegalStateException(
                    "Impossibile salvare l'invito", errore
            );
        }
    }

    private void inserisci(Invito invito) {
        GeneratedKeyHolder chiave = new GeneratedKeyHolder();

        int righe = jdbc.sql("""
                        INSERT INTO invito (team_id, destinatario_id, accettato)
                        VALUES (:teamId, :destinatarioId, :accettato)
                        """)
                .param("teamId", invito.ottieniTeam().getId())
                .param("destinatarioId", invito.getDestinatario().getId())
                .param("accettato", invito.isAccettato())
                .update(chiave, "ID");

        Number id = chiave.getKey();

        if (righe != 1 || id == null || id.longValue() <= 0) {
            throw new IllegalStateException(
                    "Inserimento invito senza identificativo valido"
            );
        }

        invito.assegnaId(id.longValue());
    }

    private void aggiorna(Invito invito) {
        int righe = jdbc.sql("""
                        UPDATE invito SET accettato = :accettato
                        WHERE id = :id
                          AND team_id = :teamId
                          AND destinatario_id = :destinatarioId
                        """)
                .param("accettato", invito.isAccettato())
                .param("id", invito.getId())
                .param("teamId", invito.ottieniTeam().getId())
                .param("destinatarioId", invito.getDestinatario().getId())
                .update();

        if (righe != 1) {
            throw new IllegalStateException(
                    "Invito assente o associazioni non corrispondenti"
            );
        }
    }

    private record RigaInvito(
            Long id,
            Long teamId,
            String nome,
            Long responsabileId,
            Utente destinatario,
            Utente membro
    ) { }
}