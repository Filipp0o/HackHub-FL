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
            List<Utente> membri,
            Utente responsabile
    ) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "Il nome del team è obbligatorio"
            );
        }

        Objects.requireNonNull(
                membri,
                "La lista dei membri è obbligatoria"
        );

        if (membri.isEmpty()) {
            throw new IllegalArgumentException(
                    "Il team deve possedere almeno un membro"
            );
        }

        if (membri.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "I membri del team non possono essere nulli"
            );
        }

        Objects.requireNonNull(
                responsabile,
                "Il responsabile è obbligatorio"
        );

        this.nome = nome;
        this.membri = new ArrayList<>(membri);

        if (this.membri.stream().noneMatch(membro ->
                stessaIdentita(membro, responsabile)
        )) {
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
        return new Team(
                nome,
                List.of(
                        Objects.requireNonNull(
                                membroIniziale,
                                "Il membro iniziale è obbligatorio"
                        )
                ),
                responsabile
        );
    }

    public static Team ricostruisci(
            Long id,
            String nome,
            List<Utente> membri,
            Utente responsabile
    ) {
        Team team = new Team(
                nome,
                membri,
                responsabile
        );

        team.assegnaId(id);
        return team;
    }

    public void assegnaId(Long id) {
        Long idValido = Objects.requireNonNull(
                id,
                "L'id del team è obbligatorio"
        );

        if (idValido <= 0) {
            throw new IllegalArgumentException(
                    "L'id del team deve essere maggiore di zero"
            );
        }

        if (this.id != null) {
            throw new IllegalStateException(
                    "L'id del team è già stato assegnato"
            );
        }

        this.id = idValido;
    }

    public int numeroMembri() {
        return membri.size();
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

    private static boolean stessaIdentita(
            Utente primo,
            Utente secondo
    ) {
        return primo == secondo
                || (
                primo.getId() != null
                        && Objects.equals(
                        primo.getId(),
                        secondo.getId()
                )
        );
    }
}