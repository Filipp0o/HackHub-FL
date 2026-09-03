package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.StatoPartecipazione;
import io.github.filipp0o.hackhub.domain.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class InMemoryPartecipazioneRepository
        implements PartecipazioneRepository {

    private final List<Partecipazione> partecipazioniSalvate =
            new ArrayList<>();

    @Override
    public List<Partecipazione> ottieniPartecipazioni(
            Hackathon hackathon
    ) {
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        return partecipazioniSalvate.stream()
                .filter(partecipazione ->
                        stessoHackathon(
                                partecipazione.getHackathon(),
                                hackathonValido
                        )
                )
                .toList();
    }

    @Override
    public List<Partecipazione>
    recuperaPartecipazioniNonEscluse(
            Hackathon hackathon
    ) {
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        return partecipazioniSalvate.stream()
                .filter(partecipazione ->
                        stessoHackathon(
                                partecipazione.getHackathon(),
                                hackathonValido
                        )
                )
                .filter(partecipazione ->
                        partecipazione.getStato()
                                != StatoPartecipazione.ESCLUSA
                )
                .toList();
    }

    @Override
    public boolean esistePartecipazione(
            Team team,
            Hackathon hackathon
    ) {
        Team teamValido = Objects.requireNonNull(
                team,
                "Il team è obbligatorio"
        );

        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        return partecipazioniSalvate.stream()
                .anyMatch(partecipazione ->
                        stessoTeam(
                                partecipazione.getTeam(),
                                teamValido
                        )
                                && stessoHackathon(
                                partecipazione.getHackathon(),
                                hackathonValido
                        )
                );
    }

    @Override
    public Partecipazione recuperaPartecipazione(
            Team team,
            Hackathon hackathon
    ) {
        Team teamValido = Objects.requireNonNull(
                team,
                "Il team è obbligatorio"
        );

        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        return partecipazioniSalvate.stream()
                .filter(partecipazione ->
                        stessoTeam(
                                partecipazione.getTeam(),
                                teamValido
                        )
                )
                .filter(partecipazione ->
                        stessoHackathon(
                                partecipazione.getHackathon(),
                                hackathonValido
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new IllegalStateException(
                                "Il team non è iscritto all'hackathon"
                        )
                );
    }

    @Override
    public void salva(
            Partecipazione partecipazione
    ) {
        Partecipazione partecipazioneValida =
                Objects.requireNonNull(
                        partecipazione,
                        "La partecipazione è obbligatoria"
                );

        for (int indice = 0;
             indice < partecipazioniSalvate.size();
             indice++) {
            if (Objects.equals(
                    partecipazioniSalvate
                            .get(indice)
                            .getId(),
                    partecipazioneValida.getId()
            )) {
                partecipazioniSalvate.set(
                        indice,
                        partecipazioneValida
                );
                return;
            }
        }

        partecipazioniSalvate.add(
                partecipazioneValida
        );
    }

    private boolean stessoHackathon(
            Hackathon primo,
            Hackathon secondo
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

    private boolean stessoTeam(
            Team primo,
            Team secondo
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