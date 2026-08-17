package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TeamRepositoryImpl implements TeamRepository {

    private final List<Team> teamSalvati = new ArrayList<>();

    @Override
    public boolean verificaAppartenenzaTeam(Utente utente) {
        Objects.requireNonNull(
                utente,
                "L'utente è obbligatorio"
        );

        return teamSalvati.stream()
                .flatMap(team -> team.getMembri().stream())
                .anyMatch(membro ->
                        membro.getId().equals(utente.getId())
                );
    }

    @Override
    public void salva(Team team) {
        teamSalvati.add(
                Objects.requireNonNull(
                        team,
                        "Il team è obbligatorio"
                )
        );
    }
}