package io.github.filipp0o.hackhub.domain;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Partecipazione {

    private static final AtomicLong SEQUENZA_ID =
            new AtomicLong(1);

    private final Long id;
    private StatoPartecipazione stato;

    private final Hackathon hackathon;
    private final Team team;

    private Sottomissione sottomissione;

    public Partecipazione(
            Hackathon hackathon,
            Team team
    ) {
        this.hackathon = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        this.team = Objects.requireNonNull(
                team,
                "Il team è obbligatorio"
        );

        this.id = SEQUENZA_ID.getAndIncrement();
        this.stato = StatoPartecipazione.ATTIVA;
    }

    public static Partecipazione crea(
            Hackathon hackathon,
            Team team
    ) {
        return new Partecipazione(
                hackathon,
                team
        );
    }

    void registraSottomissione(
            Sottomissione sottomissione
    ) {
        Sottomissione sottomissioneValida =
                Objects.requireNonNull(
                        sottomissione,
                        "La sottomissione è obbligatoria"
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