package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;
import java.util.Objects;

public class IscrivereTeamHackathonControl {

    private final HackathonRepository hackathonRepository;
    private final TeamRepository teamRepository;
    private final PartecipazioneRepository partecipazioneRepository;

    public IscrivereTeamHackathonControl(
            HackathonRepository hackathonRepository,
            TeamRepository teamRepository,
            PartecipazioneRepository partecipazioneRepository
    ) {
        this.hackathonRepository = Objects.requireNonNull(
                hackathonRepository,
                "Il repository degli hackathon è obbligatorio"
        );

        this.teamRepository = Objects.requireNonNull(
                teamRepository,
                "Il repository dei team è obbligatorio"
        );

        this.partecipazioneRepository = Objects.requireNonNull(
                partecipazioneRepository,
                "Il repository delle partecipazioni è obbligatorio"
        );
    }

    public List<Hackathon> avviaIscrizione() {
        return hackathonRepository
                .ottieniHackathonApertiAlleIscrizioni();
    }

    public Team verificaIscrizione(
            Utente utente,
            Hackathon hackathon
    ) {
        Utente utenteValido = Objects.requireNonNull(
                utente,
                "L'utente è obbligatorio"
        );

        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        Team team = Objects.requireNonNull(
                teamRepository.recuperaTeam(utenteValido),
                "Il team dell'utente è obbligatorio"
        );

        verificaAmmissibilita(
                team,
                hackathonValido
        );

        return team;
    }

    public void confermaIscrizione(
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

        /*
         * Le condizioni vengono ricontrollate nel momento
         * effettivo della registrazione.
         */
        verificaAmmissibilita(
                teamValido,
                hackathonValido
        );

        Partecipazione partecipazione =
                Partecipazione.crea(
                        hackathonValido,
                        teamValido
                );

        partecipazioneRepository.salva(
                partecipazione
        );
    }

    private void verificaAmmissibilita(
            Team team,
            Hackathon hackathon
    ) {
        if (!hackathon.isApertoAlleIscrizioni()) {
            throw new IllegalStateException(
                    "L'hackathon non è aperto alle iscrizioni"
            );
        }

        if (!hackathon.rispettaDimensioneMassima(
                team.numeroMembri()
        )) {
            throw new IllegalStateException(
                    "Il team supera la dimensione massima consentita"
            );
        }

        if (partecipazioneRepository.esistePartecipazione(
                team,
                hackathon
        )) {
            throw new IllegalStateException(
                    "Il team è già iscritto all'hackathon"
            );
        }
    }
}