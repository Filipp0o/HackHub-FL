package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;

public interface HackathonRepository {

    List<Hackathon> ottieniHackathonValutabili(
            Utente giudice
    );

    List<Hackathon> ottieniHackathonSegnalabili(
            Utente mentore
    );

    List<Hackathon> ottieniHackathonApertiAlleIscrizioni();

    void salva(Hackathon hackathon);
}