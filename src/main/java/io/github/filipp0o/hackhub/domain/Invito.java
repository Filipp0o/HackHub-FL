package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public class Invito {

    private Long id;
    private final Team team;
    private final Utente destinatario;
    private boolean accettato;

    private Invito(Team team, Utente destinatario, boolean accettato) {
        this.team = Objects.requireNonNull(team, "Il team è obbligatorio");
        this.destinatario = Objects.requireNonNull(
                destinatario, "Il destinatario è obbligatorio"
        );
        this.accettato = accettato;
    }

    public static Invito crea(Team team, Utente utenteInvitato) {
        return new Invito(team, utenteInvitato, false);
    }

    public static Invito ricostruisci(
            Long id, Team team, Utente destinatario, Boolean accettato
    ) {
        Invito invito = new Invito(
                team,
                destinatario,
                Objects.requireNonNull(accettato, "Lo stato dell'invito è obbligatorio")
        );
        invito.assegnaId(id);
        return invito;
    }

    public void assegnaId(Long id) {
        Long idValido = Objects.requireNonNull(id, "L'id dell'invito è obbligatorio");
        if (idValido <= 0) {
            throw new IllegalArgumentException(
                    "L'id dell'invito deve essere maggiore di zero"
            );
        }
        if (this.id != null) {
            throw new IllegalStateException("L'id dell'invito è già stato assegnato");
        }
        this.id = idValido;
    }

    public Team ottieniTeam() {
        return team;
    }

    public void registraAccettazione() {
        if (accettato) {
            throw new IllegalStateException("L'invito è già stato accettato");
        }
        accettato = true;
    }

    public Long getId() {
        return id;
    }

    public Utente getDestinatario() {
        return destinatario;
    }

    public boolean isAccettato() {
        return accettato;
    }
}
