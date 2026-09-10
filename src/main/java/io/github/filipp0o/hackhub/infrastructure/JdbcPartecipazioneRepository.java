package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

public class JdbcPartecipazioneRepository implements PartecipazioneRepository {

    private final JdbcClient jdbc;
    private final HackathonRepository hackathonRepository;
    private final TransactionTemplate transazioni;

    public JdbcPartecipazioneRepository(DataSource dataSource, HackathonRepository hackathonRepository) {
        Objects.requireNonNull(dataSource, "Il datasource è obbligatorio");
        this.hackathonRepository = Objects.requireNonNull(
                hackathonRepository, "Il repository degli hackathon è obbligatorio"
        );
        jdbc = JdbcClient.create(dataSource);
        var gestore = new JdbcTransactionManager(dataSource);
        gestore.setRollbackOnCommitFailure(true);
        transazioni = new TransactionTemplate(gestore);
        transazioni.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    public List<Partecipazione> ottieniPartecipazioni(Hackathon hackathon) {
        Objects.requireNonNull(hackathon, "L'hackathon è obbligatorio");
        if (hackathon.getId() == null) return List.of();
        return esegui(() -> {
            Hackathon completo = hackathonRepository.recuperaHackathon(hackathon.getId());
            return jdbc.sql("SELECT id, team_id, stato FROM partecipazione WHERE hackathon_id = :id ORDER BY id")
                    .param("id", completo.getId())
                    .query((rs, n) -> new RigaPartecipazione(
                            rs.getLong("id"), rs.getLong("team_id"),
                            StatoPartecipazione.valueOf(rs.getString("stato"))
                    )).list().stream().map(riga -> ricostruisci(riga, completo)).toList();
        });
    }

    @Override
    public List<Partecipazione> recuperaPartecipazioniNonEscluse(Hackathon hackathon) {
        return ottieniPartecipazioni(hackathon).stream()
                .filter(p -> p.getStato() != StatoPartecipazione.ESCLUSA).toList();
    }

    @Override
    public boolean esistePartecipazione(Team team, Hackathon hackathon) {
        Objects.requireNonNull(team, "Il team è obbligatorio");
        Objects.requireNonNull(hackathon, "L'hackathon è obbligatorio");
        if (team.getId() == null || hackathon.getId() == null) return false;
        return esegui(() -> jdbc.sql("""
                        SELECT COUNT(*) FROM partecipazione WHERE team_id = :team AND hackathon_id = :hackathon
                        """)
                .param("team", team.getId()).param("hackathon", hackathon.getId())
                .query(Long.class).single() != 0);
    }

    @Override
    public Partecipazione recuperaPartecipazione(Team team, Hackathon hackathon) {
        Objects.requireNonNull(team, "Il team è obbligatorio");
        return ottieniPartecipazioni(hackathon).stream()
                .filter(p -> Objects.equals(team.getId(), p.getTeam().getId())).findFirst()
                .orElseThrow(() -> new IllegalStateException("Il team non è iscritto all'hackathon"));
    }

    @Override
    public List<Partecipazione> recuperaPartecipazioniInHackathonNonConclusi(Team team) {
        Objects.requireNonNull(team, "Il team è obbligatorio");
        if (team.getId() == null) return List.of();
        return esegui(() -> jdbc.sql("""
                        SELECT p.hackathon_id FROM partecipazione p
                        JOIN hackathon h ON h.id = p.hackathon_id
                        WHERE p.team_id = :team AND h.tipo_stato <> 'CONCLUSO'
                        ORDER BY p.id
                        """)
                .param("team", team.getId()).query(Long.class).list().stream()
                .map(hackathonRepository::recuperaHackathon)
                .map(h -> recuperaPartecipazione(team, h)).toList());
    }

    private Partecipazione ricostruisci(RigaPartecipazione riga, Hackathon hackathon) {
        Partecipazione vincitrice = hackathon.getVincitrice();
        if (vincitrice != null && riga.id().equals(vincitrice.getId())) {
            if (!riga.teamId().equals(vincitrice.getTeam().getId()) || riga.stato() != vincitrice.getStato()) {
                throw new IllegalStateException("Dati della partecipazione vincitrice non coerenti");
            }
            return vincitrice;
        }
        Partecipazione p = Partecipazione.ricostruisci(
                riga.id(), hackathon, caricaTeam(riga.teamId()), riga.stato()
        );
        jdbc.sql("SELECT id, contenuto FROM sottomissione WHERE partecipazione_id = :id")
                .param("id", p.getId())
                .query((rs, n) -> Sottomissione.ricostruisci(
                        rs.getLong("id"), p, rs.getString("contenuto")
                )).optional().ifPresent(this::caricaValutazione);
        return p;
    }

    private Team caricaTeam(Long id) {
        return jdbc.sql("SELECT nome, responsabile_id FROM team WHERE id = :id")
                .param("id", id).query((rs, n) -> {
                    List<Utente> membri = jdbc.sql("""
                                    SELECT u.id, u.email, u.password_hash
                                    FROM team_membro m JOIN utente u ON u.id = m.utente_id
                                    WHERE m.team_id = :id ORDER BY u.id
                                    """)
                            .param("id", id).query((r, numero) -> Utente.ricostruisci(
                                    r.getLong("id"), r.getString("email"), r.getString("password_hash")
                            )).list();
                    long responsabileId = rs.getLong("responsabile_id");
                    Utente responsabile = membri.stream()
                            .filter(u -> u.getId() == responsabileId).findFirst()
                            .orElseThrow(() -> new IllegalStateException("Responsabile assente dai membri"));
                    return Team.ricostruisci(id, rs.getString("nome"), membri, responsabile);
                }).single();
    }

    private void caricaValutazione(Sottomissione sottomissione) {
        jdbc.sql("""
                        SELECT v.*, u.email, u.password_hash FROM valutazione v
                        JOIN utente u ON u.id = v.giudice_id WHERE v.sottomissione_id = :id
                        """)
                .param("id", sottomissione.getId())
                .query((rs, n) -> Valutazione.ricostruisci(
                        rs.getLong("id"), sottomissione,
                        Utente.ricostruisci(rs.getLong("giudice_id"), rs.getString("email"), rs.getString("password_hash")),
                        new DatiValutazione(rs.getString("giudizio"), rs.getBigDecimal("punteggio")),
                        rs.getTimestamp("data_ora").toLocalDateTime()
                )).optional();
    }

    @Override
    public void salva(Partecipazione p) {
        Objects.requireNonNull(p, "La partecipazione è obbligatoria");
        if (p.getHackathon().getId() == null || p.getTeam().getId() == null) {
            throw new IllegalArgumentException("Hackathon e team devono essere già salvati");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.getSynchronizations().stream()
                .anyMatch(s -> s instanceof AssegnazioneId a && a.entita() == p)) {
            throw new IllegalStateException("L'identificativo sarà disponibile dopo il commit della transazione");
        }
        boolean nuova = p.getId() == null;
        long id = esegui(() -> {
            if (!nuova && p.getStato() == StatoPartecipazione.ESCLUSA
                    && jdbc.sql("SELECT COUNT(*) FROM hackathon WHERE vincitrice_id = :id")
                    .param("id", p.getId()).query(Long.class).single() != 0) {
                throw new IllegalStateException("La partecipazione vincitrice non può essere esclusa");
            }
            var richiesta = jdbc.sql(nuova ? """
                            INSERT INTO partecipazione (hackathon_id, team_id, stato) VALUES (:hackathon, :team, :stato)
                            """ : """
                            UPDATE partecipazione SET stato = :stato
                            WHERE id = :id AND hackathon_id = :hackathon AND team_id = :team
                            """)
                    .param("hackathon", p.getHackathon().getId()).param("team", p.getTeam().getId())
                    .param("stato", p.getStato().name());
            if (!nuova) {
                verificaRiga(richiesta.param("id", p.getId()).update());
                return p.getId();
            }
            GeneratedKeyHolder chiave = new GeneratedKeyHolder();
            verificaRiga(richiesta.update(chiave, "ID"));
            Number generato = chiave.getKey();
            if (generato == null || generato.longValue() <= 0) {
                throw new IllegalStateException("Inserimento senza identificativo valido");
            }
            return generato.longValue();
        });
        if (nuova) {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                TransactionSynchronizationManager.registerSynchronization(new AssegnazioneId(p, id));
            } else {
                p.assegnaId(id);
            }
        }
    }

    private <T> T esegui(Supplier<T> operazione) {
        try {
            return transazioni.execute(stato -> operazione.get());
        } catch (RuntimeException errore) {
            throw new IllegalStateException("Operazione sul repository partecipazioni fallita", errore);
        }
    }

    private void verificaRiga(int righe) {
        if (righe != 1) throw new IllegalStateException("Partecipazione assente o associazioni non corrispondenti");
    }

    private record AssegnazioneId(Partecipazione entita, long id) implements TransactionSynchronization {
        @Override
        public void afterCommit() {
            entita.assegnaId(id);
        }
    }

    private record RigaPartecipazione(Long id, Long teamId, StatoPartecipazione stato) { }
}