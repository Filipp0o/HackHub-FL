package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.domain.*;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.*;

import javax.sql.DataSource;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.function.LongConsumer;
import java.util.function.Supplier;

public class JdbcSegnalazioneRepository implements SegnalazioneRepository {

    private static final String LETTURA = """
            SELECT s.*, p.hackathon_id,
                   n.id AS notifica_id, n.destinatario_id,
                   n.data_ora_creazione AS data_notifica, n.letta
            FROM segnalazione s
            JOIN partecipazione p ON p.id = s.partecipazione_id
            JOIN hackathon h ON h.id = p.hackathon_id
            LEFT JOIN notifica_segnalazione n ON n.segnalazione_id = s.id
            """;

    private final JdbcClient jdbc;
    private final HackathonRepository hackathons;
    private final PartecipazioneRepository partecipazioni;
    private final TransactionTemplate transazioni;

    public JdbcSegnalazioneRepository(
            DataSource dataSource,
            HackathonRepository hackathons,
            PartecipazioneRepository partecipazioni
    ) {
        jdbc = JdbcClient.create(Objects.requireNonNull(dataSource));
        this.hackathons = Objects.requireNonNull(hackathons);
        this.partecipazioni = Objects.requireNonNull(partecipazioni);

        var gestore = new JdbcTransactionManager(dataSource);
        gestore.setRollbackOnCommitFailure(true);

        transazioni = new TransactionTemplate(gestore);
        transazioni.setIsolationLevel(
                TransactionDefinition.ISOLATION_REPEATABLE_READ
        );
    }

    @Override
    public List<Segnalazione> ottieniSegnalazioniDaEsaminare(
            Utente organizzatore
    ) {
        Objects.requireNonNull(organizzatore, "L'organizzatore è obbligatorio");

        return esegui(() -> jdbc.sql(LETTURA + """
                        WHERE h.organizzatore_id = :utente
                          AND s.stato = 'DA_ESAMINARE'
                        ORDER BY s.id
                        """)
                .param("utente", organizzatore.getId())
                .query((rs, indice) -> ricostruisci(rs))
                .list());
    }

    @Override
    public List<NotificaSegnalazione> ottieniNotificheRicevute(
            Utente destinatario
    ) {
        Objects.requireNonNull(destinatario, "Il destinatario è obbligatorio");

        return esegui(() -> jdbc.sql(LETTURA + """
                        WHERE n.destinatario_id = :utente
                        ORDER BY n.id
                        """)
                .param("utente", destinatario.getId())
                .query((rs, indice) -> ricostruisci(rs).getNotificaSegnalazione())
                .list());
    }

    private Segnalazione ricostruisci(ResultSet rs) throws SQLException {
        Hackathon h = hackathons.recuperaHackathon(
                rs.getLong("hackathon_id")
        );
        long idPartecipazione = rs.getLong("partecipazione_id");

        Partecipazione p = partecipazioni.ottieniPartecipazioni(h).stream()
                .filter(elemento -> Objects.equals(
                        elemento.getId(), idPartecipazione
                ))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(
                        "Partecipazione non disponibile"
                ));

        String esito = rs.getString("esito");
        Long idEsaminatore = rs.getObject("esaminatore_id", Long.class);

        Segnalazione s = Segnalazione.ricostruisci(
                new DatiRipristinoSegnalazione(
                        rs.getLong("id"),
                        utente(rs.getLong("mentore_id")),
                        p,
                        rs.getString("descrizione"),
                        rs.getObject("data_ora_creazione", LocalDateTime.class),
                        StatoSegnalazione.valueOf(rs.getString("stato")),
                        esito == null ? null : EsitoSegnalazione.valueOf(esito),
                        rs.getString("motivazione"),
                        rs.getObject("data_ora_esame", LocalDateTime.class),
                        idEsaminatore == null ? null : utente(idEsaminatore)
                )
        );

        Long idNotifica = rs.getObject("notifica_id", Long.class);

        if (idNotifica != null) {
            NotificaSegnalazione.ricostruisci(
                    idNotifica,
                    s,
                    utente(rs.getLong("destinatario_id")),
                    rs.getObject("data_notifica", LocalDateTime.class),
                    rs.getBoolean("letta")
            );
        }

        return s;
    }

    private Utente utente(long id) {
        return jdbc.sql("""
                        SELECT id, email, password_hash
                        FROM utente
                        WHERE id = :id
                        """)
                .param("id", id)
                .query((rs, indice) -> Utente.ricostruisci(
                        rs.getLong("id"),
                        rs.getString("email"),
                        rs.getString("password_hash")
                ))
                .single();
    }

    @Override
    public void salva(Segnalazione segnalazione) {
        Objects.requireNonNull(segnalazione, "La segnalazione è obbligatoria");
        verificaIdInAttesa(segnalazione);

        boolean nuova = segnalazione.getId() == null;
        long id = esegui(() -> scriviSegnalazione(segnalazione));

        if (nuova) {
            assegnaDopoCommit(segnalazione, id, segnalazione::assegnaId);
        }
    }

    @Override
    public void salvaConNotifica(
            Segnalazione segnalazione,
            NotificaSegnalazione notifica
    ) {
        Objects.requireNonNull(segnalazione, "La segnalazione è obbligatoria");
        Objects.requireNonNull(notifica, "La notifica è obbligatoria");

        if (notifica.getSegnalazione() != segnalazione) {
            throw new IllegalArgumentException(
                    "La notifica deve riferirsi alla segnalazione salvata"
            );
        }

        verificaIdInAttesa(segnalazione);
        verificaIdInAttesa(notifica);

        boolean nuovaSegnalazione = segnalazione.getId() == null;
        boolean nuovaNotifica = notifica.getId() == null;

        long[] ids = esegui(() -> {
            long idSegnalazione = scriviSegnalazione(segnalazione);
            return new long[]{
                    idSegnalazione,
                    scriviNotifica(notifica, idSegnalazione)
            };
        });

        if (nuovaSegnalazione) {
            assegnaDopoCommit(segnalazione, ids[0], segnalazione::assegnaId);
        }

        if (nuovaNotifica) {
            assegnaDopoCommit(notifica, ids[1], notifica::assegnaId);
        }
    }

    @Override
    public void salvaNotifica(NotificaSegnalazione notifica) {
        Objects.requireNonNull(notifica, "La notifica è obbligatoria");
        verificaIdInAttesa(notifica);

        Long idSegnalazione = notifica.getSegnalazione().getId();

        if (idSegnalazione == null) {
            throw new IllegalStateException(
                    "La segnalazione deve essere già salvata"
            );
        }

        boolean nuova = notifica.getId() == null;
        long id = esegui(() -> scriviNotifica(notifica, idSegnalazione));

        if (nuova) {
            assegnaDopoCommit(notifica, id, notifica::assegnaId);
        }
    }

    private long scriviSegnalazione(Segnalazione s) {
        boolean nuova = s.getId() == null;

        var richiesta = jdbc.sql(nuova ? """
                        INSERT INTO segnalazione (
                            partecipazione_id, mentore_id, descrizione,
                            data_ora_creazione, stato, esito, motivazione,
                            data_ora_esame, esaminatore_id
                        )
                        VALUES (
                            :partecipazione, :mentore, :descrizione, :creazione,
                            :stato, :esito, :motivazione, :esame, :esaminatore
                        )
                        """ : """
                        UPDATE segnalazione
                        SET stato = :stato, esito = :esito,
                            motivazione = :motivazione,
                            data_ora_esame = :esame,
                            esaminatore_id = :esaminatore
                        WHERE id = :id
                          AND partecipazione_id = :partecipazione
                          AND mentore_id = :mentore
                        """)
                .param("partecipazione", s.getPartecipazione().getId())
                .param("mentore", s.getMentoreSegnalante().getId())
                .param("stato", s.getStato().name())
                .param("esito", s.getEsito() == null ? null : s.getEsito().name())
                .param("motivazione", s.getMotivazione())
                .param("esame", s.getDataOraEsame())
                .param("esaminatore",
                        s.getEsaminatore() == null
                                ? null
                                : s.getEsaminatore().getId());

        if (nuova) {
            richiesta
                    .param("descrizione", s.getDescrizione())
                    .param("creazione", s.getDataOraCreazione());
        }

        return scrivi(richiesta, s.getId());
    }

    private long scriviNotifica(
            NotificaSegnalazione n,
            long idSegnalazione
    ) {
        var richiesta = jdbc.sql(n.getId() == null ? """
                        INSERT INTO notifica_segnalazione (
                            segnalazione_id, destinatario_id,
                            data_ora_creazione, letta
                        )
                        VALUES (
                            :segnalazione, :destinatario, :creazione, :letta
                        )
                        """ : """
                        UPDATE notifica_segnalazione
                        SET letta = :letta
                        WHERE id = :id
                          AND segnalazione_id = :segnalazione
                          AND destinatario_id = :destinatario
                        """)
                .param("segnalazione", idSegnalazione)
                .param("destinatario", n.getDestinatario().getId())
                .param("letta", n.getLetta());

        if (n.getId() == null) {
            richiesta.param("creazione", n.getDataOraCreazione());
        }

        return scrivi(richiesta, n.getId());
    }

    private long scrivi(JdbcClient.StatementSpec richiesta, Long id) {
        if (id != null) {
            verificaRiga(richiesta.param("id", id).update());
            return id;
        }

        var chiave = new GeneratedKeyHolder();
        verificaRiga(richiesta.update(chiave, "ID"));

        Number valore = chiave.getKey();

        if (valore == null || valore.longValue() <= 0) {
            throw new IllegalStateException(
                    "Inserimento senza identificativo valido"
            );
        }

        return valore.longValue();
    }

    private void verificaRiga(int righe) {
        if (righe != 1) {
            throw new IllegalStateException(
                    "Oggetto assente o associazioni non corrispondenti"
            );
        }
    }

    private <T> T esegui(Supplier<T> operazione) {
        try {
            return transazioni.execute(stato -> operazione.get());
        } catch (RuntimeException errore) {
            throw new IllegalStateException(
                    "Operazione sul repository segnalazioni fallita",
                    errore
            );
        }
    }

    private void verificaIdInAttesa(Object entita) {
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.getSynchronizations()
                .stream()
                .anyMatch(s -> s instanceof AssegnazioneId a
                        && a.entita() == entita)) {
            throw new IllegalStateException(
                    "L'identificativo sarà disponibile dopo il commit della transazione"
            );
        }
    }

    private void assegnaDopoCommit(
            Object entita,
            long id,
            LongConsumer assegnazione
    ) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new AssegnazioneId(entita, id, assegnazione)
            );
        } else {
            assegnazione.accept(id);
        }
    }

    private record AssegnazioneId(
            Object entita,
            long id,
            LongConsumer assegnazione
    ) implements TransactionSynchronization {

        @Override
        public void afterCommit() {
            assegnazione.accept(id);
        }
    }
}