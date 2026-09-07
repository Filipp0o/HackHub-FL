package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public class Utente {

    private Long id;
    private final String email;
    private final String passwordHash;

    /**
     * Crea un riferimento con solo ID per i flussi esistenti.
     * Email e hash non sono disponibili in questo riferimento.
     * Per un account completo usare crea oppure ricostruisci.
     */
    public Utente(Long id) {
        this.email = null;
        this.passwordHash = null;
        assegnaId(id);
    }

    private Utente(
            String email,
            String passwordHash
    ) {
        this.email = richiediTesto(
                email,
                "L'email dell'utente è obbligatoria"
        );

        this.passwordHash = richiediTesto(
                passwordHash,
                "L'hash della password è obbligatorio"
        );
    }

    public static Utente crea(
            String email,
            String passwordHash
    ) {
        return new Utente(email, passwordHash);
    }

    public static Utente ricostruisci(
            Long id,
            String email,
            String passwordHash
    ) {
        Utente utente = new Utente(email, passwordHash);
        utente.assegnaId(id);
        return utente;
    }

    public void assegnaId(Long id) {
        Long idValido = Objects.requireNonNull(
                id,
                "L'id dell'utente è obbligatorio"
        );

        if (idValido <= 0) {
            throw new IllegalArgumentException(
                    "L'id dell'utente deve essere maggiore di zero"
            );
        }

        if (this.id != null) {
            throw new IllegalStateException(
                    "L'id dell'utente è già stato assegnato"
            );
        }

        this.id = idValido;
    }

    public String recuperaEmail() {
        return email;
    }

    public String recuperaPasswordHash() {
        return passwordHash;
    }

    public Long getId() {
        return id;
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
