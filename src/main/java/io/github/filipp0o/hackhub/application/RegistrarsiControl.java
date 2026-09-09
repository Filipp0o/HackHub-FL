package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Utente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

public class RegistrarsiControl {

    private static final Pattern FORMATO_EMAIL = Pattern.compile(
            "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+"
                    + "(?:\\.[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+)*@"
                    + "[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?"
                    + "(?:\\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+"
    );

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

        boolean emailEsistente;
        try {
            emailEsistente = utenteRepository.esistePerEmail(email);
        } catch (RuntimeException causa) {
            throw new RegistrazioneFallitaException(causa);
        }

        if (emailEsistente) {
            throw new EmailGiaRegistrataException();
        }

        String passwordHash = codificatorePassword.codifica(password);

        try {
            Utente utente = Utente.crea(email, passwordHash);
            utenteRepository.salva(utente);
        } catch (RuntimeException causa) {
            throw new RegistrazioneFallitaException(causa);
        }
    }

    public void verificaDatiRegistrazione(String email, String password) {
        List<String> errori = new ArrayList<>();

        if (email == null || email.isBlank()) {
            errori.add("L'email è obbligatoria");
        } else if (email.length() > 320
                || !FORMATO_EMAIL.matcher(email).matches()) {
            errori.add("Il formato dell'email non è valido");
        }

        if (password == null || password.isBlank()) {
            errori.add("La password è obbligatoria");
        }

        if (!errori.isEmpty()) {
            throw new IllegalArgumentException(String.join("; ", errori));
        }
    }

    public static class RegistrazioneFallitaException
            extends IllegalStateException {

        public RegistrazioneFallitaException(Throwable causa) {
            super("Registrazione non completata", causa);
        }
    }

    public static class EmailGiaRegistrataException
            extends IllegalStateException {

        public EmailGiaRegistrataException() {
            super("L'email è già registrata");
        }
    }
}