package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.PartecipazioneRepository;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.StatoPartecipazione;
import io.github.filipp0o.hackhub.domain.Team;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PartecipazioneRepositoryImpl
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
                        partecipazione.getHackathon()
                                == hackathonValido
                )
                .toList();
    }

    @Override
    public List<Partecipazione> recuperaPartecipazioniNonEscluse(
            Hackathon hackathon
    ) {
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        return partecipazioniSalvate.stream()
                .filter(partecipazione ->
                        partecipazione.getHackathon()
                                == hackathonValido
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
                        partecipazione.getTeam()
                                == teamValido
                                && partecipazione.getHackathon()
                                == hackathonValido
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
                        partecipazione.getTeam()
                                == teamValido
                )
                .filter(partecipazione ->
                        partecipazione.getHackathon()
                                == hackathonValido
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
        partecipazioniSalvate.add(
                Objects.requireNonNull(
                        partecipazione,
                        "La partecipazione è obbligatoria"
                )
        );
    }
}