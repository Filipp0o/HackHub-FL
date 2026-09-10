package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.application.SottomissioneRepository;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
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

public class JdbcSottomissioneRepository implements SottomissioneRepository {

    private final JdbcClient jdbc;
    private final PartecipazioneRepository partecipazioneRepository;
    private final TransactionTemplate transazioni;

    public JdbcSottomissioneRepository(
            DataSource dataSource,
            PartecipazioneRepository partecipazioneRepository
    ) {
        Objects.requireNonNull(dataSource, "Il datasource è obbligatorio");
        this.partecipazioneRepository = Objects.requireNonNull(
                partecipazioneRepository,
                "Il repository delle partecipazioni è obbligatorio"
        );
        jdbc = JdbcClient.create(dataSource);
        var gestore = new JdbcTransactionManager(dataSource);
        gestore.setRollbackOnCommitFailure(true);
        transazioni = new TransactionTemplate(gestore);
        transazioni.setIsolationLevel(TransactionDefinition.ISOLATION_REPEATABLE_READ);
    }

    @Override
    public Sottomissione recuperaSottomissione(Partecipazione partecipazione) {
        Objects.requireNonNull(partecipazione, "La partecipazione è obbligatoria");
        if (partecipazione.getId() == null) {
            throw new IllegalStateException("Sottomissione non trovata");
        }
        return esegui(() -> {
            Partecipazione completa = partecipazioneRepository.recuperaPartecipazione(
                    partecipazione.getTeam(), partecipazione.getHackathon()
            );
            if (!Objects.equals(partecipazione.getId(), completa.getId())
                    || completa.getSottomissione() == null) {
                throw new IllegalStateException("Sottomissione non trovata");
            }
            return completa.getSottomissione();
        });
    }

    @Override
    public void salva(Sottomissione sottomissione) {
        Objects.requireNonNull(sottomissione, "La sottomissione è obbligatoria");
        if (sottomissione.getPartecipazione().getId() == null) {
            throw new IllegalArgumentException("La partecipazione deve essere già salvata");
        }
        if (TransactionSynchronizationManager.isSynchronizationActive()
                && TransactionSynchronizationManager.getSynchronizations().stream()
                .anyMatch(s -> s instanceof AssegnazioneId a && a.entita() == sottomissione)) {
            throw new IllegalStateException(
                    "L'identificativo sarà disponibile dopo il commit della transazione"
            );
        }
        boolean nuova = sottomissione.getId() == null;
        long id = esegui(() -> {
            var richiesta = jdbc.sql(nuova ? """
                            INSERT INTO sottomissione (partecipazione_id, contenuto)
                            VALUES (:partecipazione, :contenuto)
                            """ : """
                            UPDATE sottomissione SET contenuto = :contenuto
                            WHERE id = :id AND partecipazione_id = :partecipazione
                            """)
                    .param("partecipazione", sottomissione.getPartecipazione().getId())
                    .param("contenuto", sottomissione.getContenuto());
            if (!nuova) {
                verificaRiga(richiesta.param("id", sottomissione.getId()).update());
                return sottomissione.getId();
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
                        new AssegnazioneId(sottomissione, id)
                );
            } else {
                sottomissione.assegnaId(id);
            }
        }
    }

    private <T> T esegui(Supplier<T> operazione) {
        try {
            return transazioni.execute(stato -> operazione.get());
        } catch (RuntimeException errore) {
            throw new IllegalStateException("Operazione sul repository sottomissioni fallita", errore);
        }
    }

    private void verificaRiga(int righe) {
        if (righe != 1) {
            throw new IllegalStateException("Sottomissione assente o partecipazione non corrispondente");
        }
    }

    private record AssegnazioneId(Sottomissione entita, long id)
            implements TransactionSynchronization {
        @Override
        public void afterCommit() {
            entita.assegnaId(id);
        }
    }
}