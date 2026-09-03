package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InMemoryTeamRepository
        implements TeamRepository {

    private final List<Team> teamSalvati =
            new ArrayList<>();

    private long prossimoId = 1L;

    @Override
    public boolean verificaAppartenenzaTeam(
            Utente utente
    ) {
        Utente utenteValido = Objects.requireNonNull(
                utente,
                "L'utente è obbligatorio"
        );

        return teamSalvati.stream()
                .flatMap(team ->
                        team.getMembri().stream()
                )
                .anyMatch(membro ->
                        stessaIdentita(
                                membro,
                                utenteValido
                        )
                );
    }

    @Override
    public Team recuperaTeam(
            Utente utente
    ) {
        Utente utenteValido = Objects.requireNonNull(
                utente,
                "L'utente è obbligatorio"
        );

        return teamSalvati.stream()
                .filter(team ->
                        team.getMembri().stream()
                                .anyMatch(membro ->
                                        stessaIdentita(
                                                membro,
                                                utenteValido
                                        )
                                )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "L'utente non appartiene ad alcun team"
                        )
                );
    }

    @Override
    public void salva(
            Team team
    ) {
        Team teamValido = Objects.requireNonNull(
                team,
                "Il team è obbligatorio"
        );

        if (teamValido.getId() == null) {
            teamValido.assegnaId(prossimoId++);
        } else {
            prossimoId = Math.max(
                    prossimoId,
                    teamValido.getId() + 1
            );
        }

        for (int indice = 0;
             indice < teamSalvati.size();
             indice++) {
            if (Objects.equals(
                    teamSalvati.get(indice).getId(),
                    teamValido.getId()
            )) {
                teamSalvati.set(
                        indice,
                        teamValido
                );
                return;
            }
        }

        teamSalvati.add(teamValido);
    }

    private boolean stessaIdentita(
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