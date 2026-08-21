package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.StatoPartecipazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.Objects;

public class InviareSottomissioneControl {

    private final TeamRepository teamRepository;
    private final PartecipazioneRepository partecipazioneRepository;
    private final SottomissioneRepository sottomissioneRepository;

    public InviareSottomissioneControl(
            TeamRepository teamRepository,
            PartecipazioneRepository partecipazioneRepository,
            SottomissioneRepository sottomissioneRepository
    ) {
        this.teamRepository = Objects.requireNonNull(
                teamRepository,
                "Il repository dei team è obbligatorio"
        );

        this.partecipazioneRepository = Objects.requireNonNull(
                partecipazioneRepository,
                "Il repository delle partecipazioni è obbligatorio"
        );

        this.sottomissioneRepository = Objects.requireNonNull(
                sottomissioneRepository,
                "Il repository delle sottomissioni è obbligatorio"
        );
    }

    public Partecipazione avviaInvioSottomissione(
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

        Partecipazione partecipazione = Objects.requireNonNull(
                partecipazioneRepository.recuperaPartecipazione(
                        team,
                        hackathonValido
                ),
                "La partecipazione è obbligatoria"
        );

        verificaPartecipazione(partecipazione);

        return partecipazione;
    }

    public void verificaContenuto(
            String contenuto
    ) {
        if (contenuto == null || contenuto.isBlank()) {
            throw new IllegalArgumentException(
                    "Il contenuto della sottomissione è obbligatorio"
            );
        }
    }

    public void inviaSottomissione(
            Partecipazione partecipazione,
            String contenuto
    ) {
        Partecipazione partecipazioneValida =
                Objects.requireNonNull(
                        partecipazione,
                        "La partecipazione è obbligatoria"
                );

        verificaContenuto(contenuto);

        /*
         * Ricontrolliamo le condizioni nel momento
         * effettivo della modifica.
         */
        verificaPartecipazione(
                partecipazioneValida
        );

        Hackathon hackathon =
                partecipazioneValida.ottieniHackathon();

        if (!hackathon.scadenzaSottomissioneNonTrascorsa()) {
            throw new IllegalStateException(
                    "La scadenza per la sottomissione è trascorsa"
            );
        }

        Sottomissione sottomissione =
                Sottomissione.crea(
                        partecipazioneValida,
                        contenuto
                );

        sottomissioneRepository.salva(
                sottomissione
        );
    }

    private void verificaPartecipazione(
            Partecipazione partecipazione
    ) {
        if (partecipazione.getStato()
                != StatoPartecipazione.ATTIVA) {
            throw new IllegalStateException(
                    "La partecipazione non è attiva"
            );
        }

        if (partecipazione.getSottomissione() != null) {
            throw new IllegalStateException(
                    "È già presente una sottomissione per questa partecipazione"
            );
        }
    }
}