package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.Objects;

public interface TeamRepository {

    boolean verificaAppartenenzaTeam(Utente utente);

    Team recuperaTeam(Utente utente);

    default Team recuperaTeamCreatoDa(Utente utente) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");

        if (!verificaAppartenenzaTeam(utente)) {
            throw new TeamNonCreatoException();
        }

        Team team = recuperaTeam(utente);
        Utente responsabile = team.getResponsabile();

        if (responsabile != utente
                && (utente.getId() == null
                || !utente.getId().equals(responsabile.getId()))) {
            throw new TeamNonCreatoException();
        }

        return team;
    }

    void salva(Team team);

    class TeamNonCreatoException extends IllegalStateException {

        public TeamNonCreatoException() {
            super("L'utente non ha creato un team");
        }
    }
}