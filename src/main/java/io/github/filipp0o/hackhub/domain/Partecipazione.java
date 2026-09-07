package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public class Partecipazione {

    private Long id;
    private StatoPartecipazione stato;

    private final Hackathon hackathon;
    private final Team team;

    private Sottomissione sottomissione;

    public Partecipazione(Hackathon hackathon, Team team) {
        this.hackathon = Objects.requireNonNull(
                hackathon, "L'hackathon è obbligatorio"
        );
        this.team = Objects.requireNonNull(
                team, "Il team è obbligatorio"
        );
        this.stato = StatoPartecipazione.ATTIVA;
    }

    public static Partecipazione crea(Hackathon hackathon, Team team) {
        return new Partecipazione(hackathon, team);
    }

    void registraSottomissione(Sottomissione sottomissione) {
        Sottomissione sottomissioneValida = Objects.requireNonNull(
                sottomissione, "La sottomissione è obbligatoria"
        );

        if (this.sottomissione != null) {
            throw new IllegalStateException(
                    "La partecipazione possiede già una sottomissione"
            );
        }

        this.sottomissione = sottomissioneValida;
    }

    public Hackathon ottieniHackathon() {
        return hackathon;
    }

    public void assegnaId(Long id) {
        Long idValido = Objects.requireNonNull(
                id, "L'id della partecipazione è obbligatorio"
        );

        if (idValido <= 0) {
            throw new IllegalArgumentException(
                    "L'id della partecipazione deve essere maggiore di zero"
            );
        }

        if (this.id != null) {
            throw new IllegalStateException(
                    "L'id della partecipazione è già stato assegnato"
            );
        }

        this.id = idValido;
    }

    public Long getId() {
        return id;
    }

    public StatoPartecipazione getStato() {
        return stato;
    }

    public Hackathon getHackathon() {
        return hackathon;
    }

    public Team getTeam() {
        return team;
    }

    public Sottomissione getSottomissione() {
        return sottomissione;
    }

    public void escludi() {
        this.stato = StatoPartecipazione.ESCLUSA;
    }
}