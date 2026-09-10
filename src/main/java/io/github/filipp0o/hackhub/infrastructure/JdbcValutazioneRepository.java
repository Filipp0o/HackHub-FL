package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.ValutazioneRepository;
import io.github.filipp0o.hackhub.domain.Valutazione;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.util.Objects;
import java.util.function.Supplier;

public class JdbcValutazioneRepository implements ValutazioneRepository {

    private final JdbcClient jdbc;
    private final TransactionTemplate transazioni;

    public JdbcValutazioneRepository(DataSource dataSource) {
        Objects.requireNonNull(dataSource, "Il datasource è obbligatorio");
        jdbc = JdbcClient.create(dataSource);
        var gestore = new JdbcTransactionManager(dataSource);
        gestore.setRollbackOnCommitFailure(true);
        transazioni = new TransactionTemplate(gestore);
        transazioni.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    public void salva(Valutazione valutazione) {
        Objects.requireNonNull(valutazione, "La valutazione è obbligatoria");
        if (valutazione.getSottomissione().getId() == null
                || valutazione.getGiudice().getId() == null) {
            throw new IllegalArgumentException("Sottomissione e giudice devono essere già salvati");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.getSynchronizations().stream()
                .anyMatch(s -> s instanceof AssegnazioneId a && a.entita() == valutazione)) {
            throw new IllegalStateException(
                    "L'identificativo sarà disponibile dopo il commit della transazione"
            );
        }
        boolean nuova = valutazione.getId() == null;
        long id = esegui(() -> {
            var richiesta = jdbc.sql(nuova ? """
                            INSERT INTO valutazione
                                (sottomissione_id, giudice_id, giudizio, punteggio, data_ora)
                            VALUES (:sottomissione, :giudice, :giudizio, :punteggio, :data)
                            """ : """
                            UPDATE valutazione
                            SET giudizio = :giudizio, punteggio = :punteggio, data_ora = :data
                            WHERE id = :id AND sottomissione_id = :sottomissione AND giudice_id = :giudice
                            """)
                    .param("sottomissione", valutazione.getSottomissione().getId())
                    .param("giudice", valutazione.getGiudice().getId())
                    .param("giudizio", valutazione.getGiudizio())
                    .param("punteggio", valutazione.getPunteggio())
                    .param("data", valutazione.getDataOra());
            if (!nuova) {
                verificaRiga(richiesta.param("id", valutazione.getId()).update());
                return valutazione.getId();
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
                TransactionSynchronizationManager.registerSynchronization(
                        new AssegnazioneId(valutazione, id)
                );
            } else {
                valutazione.assegnaId(id);
            }
        }
    }

    private <T> T esegui(Supplier<T> operazione) {
        try {
            return transazioni.execute(stato -> operazione.get());
        } catch (RuntimeException errore) {
            throw new IllegalStateException("Operazione sul repository valutazioni fallita", errore);
        }
    }

    private void verificaRiga(int righe) {
        if (righe != 1) {
            throw new IllegalStateException("Valutazione assente o associazioni non corrispondenti");
        }
    }

    private record AssegnazioneId(Valutazione entita, long id)
            implements TransactionSynchronization {
        @Override
        public void afterCommit() {
            entita.assegnaId(id);
        }
    }
}