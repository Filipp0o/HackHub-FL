package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.domain.Utente;

import java.util.Objects;

public class SessioneUtente {

    private volatile Utente utenteAutenticato;

    public void registra(Utente utente) {
        utenteAutenticato = Objects.requireNonNull(
                utente,
                "L'utente autenticato è obbligatorio"
        );
    }

    /**
     * @throws UtenteNonAutenticatoException se la sessione non contiene un utente
     */
    public Utente recupera() {
        Utente utente = utenteAutenticato;
        if (utente == null) {
            throw new UtenteNonAutenticatoException();
        }
        return utente;
    }

    public void svuota() {
        utenteAutenticato = null;
    }

    public static class UtenteNonAutenticatoException
            extends IllegalStateException {

        public UtenteNonAutenticatoException() {
            super("Nessun utente autenticato nella sessione");
        }
    }
}
