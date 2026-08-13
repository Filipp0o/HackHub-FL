package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.Utente;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class SegnalareViolazioneControl {

    private final HackathonRepository hackathonRepository;
    private final PartecipazioneRepository partecipazioneRepository;
    private final SegnalazioneRepository segnalazioneRepository;

    public SegnalareViolazioneControl(
            HackathonRepository hackathonRepository,
            PartecipazioneRepository partecipazioneRepository,
            SegnalazioneRepository segnalazioneRepository
    ) {
        this.hackathonRepository = Objects.requireNonNull(
                hackathonRepository,
                "Il repository degli hackathon è obbligatorio"
        );

        this.partecipazioneRepository = Objects.requireNonNull(
                partecipazioneRepository,
                "Il repository delle partecipazioni è obbligatorio"
        );

        this.segnalazioneRepository = Objects.requireNonNull(
                segnalazioneRepository,
                "Il repository delle segnalazioni è obbligatorio"
        );
    }

    public List<Hackathon> avviaSegnalazioneViolazione(
            Utente mentore
    ) {
        Utente mentoreValido = Objects.requireNonNull(
                mentore,
                "Il mentore è obbligatorio"
        );

        return hackathonRepository.ottieniHackathonSegnalabili(
                mentoreValido
        );
    }

    public List<Partecipazione> selezionaHackathon(
            Hackathon hackathon
    ) {
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        return partecipazioneRepository.ottieniPartecipazioni(
                hackathonValido
        );
    }

    public String selezionaTeam(
            Partecipazione partecipazione
    ) {
        Partecipazione partecipazioneValida =
                Objects.requireNonNull(
                        partecipazione,
                        "La partecipazione è obbligatoria"
                );

        return partecipazioneValida
                .getHackathon()
                .getRegolamento();
    }

    public void verificaDescrizione(
            String descrizione
    ) {
        if (descrizione == null || descrizione.isBlank()) {
            throw new IllegalArgumentException(
                    "La descrizione della violazione è obbligatoria"
            );
        }
    }

    public void registraSegnalazioneConNotifica(
            Utente mentore,
            Partecipazione partecipazione,
            String descrizione
    ) {
        Utente mentoreValido = Objects.requireNonNull(
                mentore,
                "Il mentore è obbligatorio"
        );

        Partecipazione partecipazioneValida =
                Objects.requireNonNull(
                        partecipazione,
                        "La partecipazione è obbligatoria"
                );

        verificaDescrizione(descrizione);

        Hackathon hackathon =
                partecipazioneValida.getHackathon();

        hackathon.aggiornaStato(LocalDate.now());

        Segnalazione segnalazione = Segnalazione.crea(
                mentoreValido,
                partecipazioneValida,
                descrizione
        );

        NotificaSegnalazione notifica =
                NotificaSegnalazione.crea(
                        segnalazione,
                        hackathon.getOrganizzatore()
                );

        segnalazioneRepository.salvaConNotifica(
                segnalazione,
                notifica
        );
    }
}