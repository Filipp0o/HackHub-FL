package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

public interface TeamRepository {

    boolean verificaAppartenenzaTeam(Utente utente);

    Team recuperaTeam(Utente utente);

    /**
     * Recupera il team di cui l'utente è responsabile, ruolo assegnato al creatore.
     * @throws IllegalStateException se l'utente non appartiene a un team
     * oppure non ne è il responsabile
     */
    default Team recuperaTeamCreatoDa(Utente utente) {
        java.util.Objects.requireNonNull(utente, "L'utente è obbligatorio");
        Team team = recuperaTeam(utente);
        Utente responsabile = team.getResponsabile();
        if (responsabile != utente
                && (utente.getId() == null
                || !utente.getId().equals(responsabile.getId()))) {
            throw new IllegalStateException("L'utente non ha creato il team");
        }
        return team;
    }

    void salva(Team team);
}
