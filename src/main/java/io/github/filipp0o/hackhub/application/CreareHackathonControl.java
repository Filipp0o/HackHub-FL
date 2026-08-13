package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;
import java.util.Objects;

public class CreareHackathonControl {

    private final UtenteRepository utenteRepository;
    private final HackathonRepository hackathonRepository;

    public CreareHackathonControl(
            UtenteRepository utenteRepository,
            HackathonRepository hackathonRepository
    ) {
        this.utenteRepository = Objects.requireNonNull(
                utenteRepository,
                "Il repository degli utenti è obbligatorio"
        );
        this.hackathonRepository = Objects.requireNonNull(
                hackathonRepository,
                "Il repository degli hackathon è obbligatorio"
        );
    }

    public List<Utente> recuperaUtentiAssegnabili() {
        return utenteRepository.recuperaUtentiAssegnabili();
    }

    public void crea(
            DatiHackathon dati,
            Utente organizzatore,
            Utente giudice,
            List<Utente> mentori
    ) {
        Hackathon hackathon = Hackathon.crea(
                dati,
                organizzatore,
                giudice,
                mentori
        );

        hackathonRepository.salva(hackathon);
    }
}