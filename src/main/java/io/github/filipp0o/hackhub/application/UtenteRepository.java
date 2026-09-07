package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;

public interface UtenteRepository {

    List<Utente> recuperaUtentiAssegnabili();

    /** Recupera gli altri account registrati, escludendo i riferimenti con solo ID. */
    default List<Utente> recuperaUtentiInvitabili(Utente utente) {
        java.util.Objects.requireNonNull(utente, "L'utente è obbligatorio");
        if (utente.getId() == null) {
            throw new IllegalArgumentException("L'utente deve avere un ID");
        }

        return recuperaUtentiAssegnabili().stream()
                .filter(candidato -> !utente.getId().equals(candidato.getId()))
                .filter(candidato -> candidato.recuperaEmail() != null
                        && candidato.recuperaPasswordHash() != null)
                .toList();
    }

    boolean esistePerEmail(String email);

    /**
     * Recupera l'account corrispondente all'email, oppure null se assente.
     *
     * @throws IllegalArgumentException se l'email è nulla o vuota
     * @throws IllegalStateException se il recupero non può essere completato
     */
    Utente recuperaPerEmail(String email);

    void salva(Utente utente);
}
