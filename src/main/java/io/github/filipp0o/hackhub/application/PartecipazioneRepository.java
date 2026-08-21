package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Team;

import java.util.List;

public interface PartecipazioneRepository {

    List<Partecipazione> ottieniPartecipazioni(
            Hackathon hackathon
    );

    List<Partecipazione> recuperaPartecipazioniNonEscluse(
            Hackathon hackathon
    );

    boolean esistePartecipazione(
            Team team,
            Hackathon hackathon
    );

    Partecipazione recuperaPartecipazione(
            Team team,
            Hackathon hackathon
    );

    void salva(Partecipazione partecipazione);
}