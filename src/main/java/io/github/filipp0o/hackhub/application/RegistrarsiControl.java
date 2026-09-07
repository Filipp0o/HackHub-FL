package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;

import java.util.Objects;

public class RegistrarsiControl {

    private final UtenteRepository utenteRepository;
    private final CodificatorePassword codificatorePassword;

    public RegistrarsiControl(
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

    public void richiediRegistrazione(String email, String password) {
        verificaDatiRegistrazione(email, password);

        if (utenteRepository.esistePerEmail(email)) {
            throw new EmailGiaRegistrataException();
        }

        String passwordHash = codificatorePassword.codifica(password);
        Utente utente = Utente.crea(email, passwordHash);
        utenteRepository.salva(utente);
    }

    public void verificaDatiRegistrazione(String email, String password) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("L'email è obbligatoria");
        }

        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La password è obbligatoria");
        }
    }

    public static class EmailGiaRegistrataException
            extends IllegalStateException {

        public EmailGiaRegistrataException() {
            super("L'email è già registrata");
        }
    }
}