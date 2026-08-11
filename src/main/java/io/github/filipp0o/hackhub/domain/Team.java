package io.github.filipp0o.hackhub.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class Team {

    private Long id;
    private final String nome;
    private final List<Utente> membri;
    private final Utente responsabile;

    private Team(
            String nome,
            Utente membroIniziale,
            Utente responsabile
    ) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "Il nome del team è obbligatorio"
            );
        }

        Objects.requireNonNull(
                membroIniziale,
                "Il membro iniziale è obbligatorio"
        );
        Objects.requireNonNull(
                responsabile,
                "Il responsabile è obbligatorio"
        );

        this.nome = nome;
        this.membri = new ArrayList<>();
        this.membri.add(membroIniziale);

        if (!this.membri.contains(responsabile)) {
            throw new IllegalArgumentException(
                    "Il responsabile deve appartenere ai membri del team"
            );
        }

        this.responsabile = responsabile;
    }

    public static Team crea(
            String nome,
            Utente membroIniziale,
            Utente responsabile
    ) {
        return new Team(nome, membroIniziale, responsabile);
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public List<Utente> getMembri() {
        return List.copyOf(membri);
    }

    public Utente getResponsabile() {
        return responsabile;
    }
}