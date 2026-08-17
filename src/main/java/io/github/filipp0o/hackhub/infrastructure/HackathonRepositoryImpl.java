package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class HackathonRepositoryImpl implements HackathonRepository {

    private final List<Hackathon> hackathonSalvati =
            new ArrayList<>();

    @Override
    public List<Hackathon> ottieniHackathonValutabili(
            Utente giudice
    ) {
        Utente giudiceValido = Objects.requireNonNull(
                giudice,
                "Il giudice è obbligatorio"
        );

        return hackathonSalvati.stream()
                .filter(hackathon ->
                        hackathon.getStato()
                                == StatoHackathon.IN_VALUTAZIONE
                )
                .filter(hackathon ->
                        Objects.equals(
                                hackathon.getGiudice().getId(),
                                giudiceValido.getId()
                        )
                )
                .toList();
    }

    @Override
    public List<Hackathon> ottieniHackathonSegnalabili(
            Utente mentore
    ) {
        Utente mentoreValido = Objects.requireNonNull(
                mentore,
                "Il mentore è obbligatorio"
        );

        return hackathonSalvati.stream()
                .filter(hackathon ->
                        hackathon.getStato()
                                == StatoHackathon.IN_CORSO
                                || hackathon.getStato()
                                == StatoHackathon.IN_VALUTAZIONE
                )
                .filter(hackathon ->
                        hackathon.getMentori().stream()
                                .anyMatch(mentoreAssegnato ->
                                        Objects.equals(
                                                mentoreAssegnato.getId(),
                                                mentoreValido.getId()
                                        )
                                )
                )
                .toList();
    }

    @Override
    public void salva(Hackathon hackathon) {
        hackathonSalvati.add(
                Objects.requireNonNull(
                        hackathon,
                        "L'hackathon è obbligatorio"
                )
        );
    }
}