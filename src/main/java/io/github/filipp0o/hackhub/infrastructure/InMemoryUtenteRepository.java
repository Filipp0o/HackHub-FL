package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.UtenteRepository;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class InMemoryUtenteRepository implements UtenteRepository {

    private final Map<Long, Utente> utenti = new LinkedHashMap<>();
    private long ultimoId;

    public InMemoryUtenteRepository(List<Utente> utentiIniziali) {
        Objects.requireNonNull(
                utentiIniziali,
                "La lista degli utenti è obbligatoria"
        );

        for (Utente utente : utentiIniziali) {
            Objects.requireNonNull(
                    utente,
                    "L'utente è obbligatorio"
            );

            Long id = Objects.requireNonNull(
                    utente.getId(),
                    "Gli utenti iniziali devono avere un ID"
            );

            if (utenti.containsKey(id)) {
                throw new IllegalArgumentException(
                        "ID utente iniziale duplicato"
                );
            }

            verificaEmailDisponibile(utente);
            utenti.put(id, utente);
            ultimoId = Math.max(ultimoId, id);
        }
    }

    @Override
    public List<Utente> recuperaUtentiAssegnabili() {
        return List.copyOf(utenti.values());
    }

    @Override
    public boolean esistePerEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException(
                    "L'email è obbligatoria"
            );
        }

        return utenti.values().stream()
                .anyMatch(utente ->
                        email.equals(utente.recuperaEmail())
                );
    }

    @Override
    public void salva(Utente utente) {
        Objects.requireNonNull(
                utente,
                "L'utente è obbligatorio"
        );

        if (utente.recuperaEmail() == null
                || utente.recuperaEmail().isBlank()
                || utente.recuperaPasswordHash() == null
                || utente.recuperaPasswordHash().isBlank()) {
            throw new IllegalArgumentException(
                    "Il salvataggio richiede un account con email e hash"
            );
        }

        Long id = utente.getId();

        if (id != null && !utenti.containsKey(id)) {
            throw new IllegalStateException(
                    "L'utente da aggiornare non esiste"
            );
        }

        verificaEmailDisponibile(utente);

        if (id == null) {
            if (ultimoId == Long.MAX_VALUE) {
                throw new IllegalStateException(
                        "Identificativi utente esauriti"
                );
            }

            id = ultimoId + 1;
            utente.assegnaId(id);
            ultimoId = id;
        }

        utenti.put(id, utente);
    }

    private void verificaEmailDisponibile(Utente candidato) {
        String email = candidato.recuperaEmail();

        if (email == null) {
            // Riferimenti con solo ID usati nei flussi esistenti.
            return;
        }

        boolean duplicata = utenti.values().stream()
                .anyMatch(esistente ->
                        email.equals(esistente.recuperaEmail())
                                && !Objects.equals(
                                esistente.getId(),
                                candidato.getId()
                        )
                );

        if (duplicata) {
            throw new IllegalStateException(
                    "L'email è già registrata"
            );
        }
    }
}