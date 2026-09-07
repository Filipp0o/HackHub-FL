package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;

import java.util.Objects;

public class EffettuareAccessoControl {

    private final UtenteRepository utenteRepository;
    private final CodificatorePassword codificatorePassword;

    public EffettuareAccessoControl(
            UtenteRepository utenteRepository,
            CodificatorePassword codificatorePassword
    ) {
        this.utenteRepository = Objects.requireNonNull(
                utenteRepository,
                "Il repository degli utenti è obbligatorio"
        );
        this.codificatorePassword = Objects.requireNonNull(
                codificatorePassword,
                "Il codificatore delle password è obbligatorio"
        );
    }

    public Utente richiediAccesso(String email, String password) {
        verificaDatiAccesso(email, password);

        Utente utente = utenteRepository.recuperaPerEmail(email);

        if (utente == null) {
            throw new CredenzialiNonValideException();
        }

        if (!codificatorePassword.verifica(
                password,
                utente.recuperaPasswordHash()
        )) {
            throw new CredenzialiNonValideException();
        }

        return utente;
    }

    private void verificaDatiAccesso(String email, String password) {
        if (email == null || email.isBlank()
                || password == null || password.isBlank()) {
            throw new CredenzialiNonValideException();
        }
    }

    public static class CredenzialiNonValideException
            extends IllegalArgumentException {

        public CredenzialiNonValideException() {
            super("Credenziali non valide");
        }
    }
}
