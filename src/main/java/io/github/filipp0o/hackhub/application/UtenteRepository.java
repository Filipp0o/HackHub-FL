package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;

public interface UtenteRepository {

    List<Utente> recuperaUtentiAssegnabili();

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
