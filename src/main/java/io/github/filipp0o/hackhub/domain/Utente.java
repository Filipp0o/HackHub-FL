package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public class Utente {

    private final Long id;

    public Utente(Long id) {
        this.id = Objects.requireNonNull(id, "L'id dell'utente è obbligatorio");
    }

    public Long getId() {
        return id;
    }
}