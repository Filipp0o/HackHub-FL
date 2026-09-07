package io.github.filipp0o.hackhub.domain;

public final class Credenziali {

    private final String email;
    private final String passwordHash;

    private Credenziali(String email, String passwordHash) {
        this.email = richiediTesto(
                email,
                "L'email dell'utente è obbligatoria"
        );

        this.passwordHash = richiediTesto(
                passwordHash,
                "L'hash della password è obbligatorio"
        );
    }

    public static Credenziali crea(
            String email,
            String passwordHash
    ) {
        return new Credenziali(email, passwordHash);
    }

    String recuperaEmail() {
        return email;
    }

    String recuperaPasswordHash() {
        return passwordHash;
    }

    private static String richiediTesto(
            String valore,
            String messaggio
    ) {
        if (valore == null || valore.isBlank()) {
            throw new IllegalArgumentException(messaggio);
        }

        return valore;
    }
}