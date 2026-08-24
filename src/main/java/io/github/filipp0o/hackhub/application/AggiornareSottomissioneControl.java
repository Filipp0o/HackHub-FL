package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.StatoPartecipazione;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.Objects;

public class AggiornareSottomissioneControl {

    private final TeamRepository teamRepository;
    private final PartecipazioneRepository partecipazioneRepository;
    private final SottomissioneRepository sottomissioneRepository;

    public AggiornareSottomissioneControl(
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

    public String avviaAggiornamentoSottomissione(
            Utente utente,
            Hackathon hackathon
    ) {
        Sottomissione sottomissione =
                recuperaSottomissione(
                        utente,
                        hackathon
                );

        return sottomissione.ottieniContenuto();
    }

    public void verificaContenuto(
            String nuovoContenuto
    ) {
        if (nuovoContenuto == null
                || nuovoContenuto.isBlank()) {
            throw new IllegalArgumentException(
                    "Il contenuto della sottomissione è obbligatorio"
            );
        }
    }

    public void aggiornaSottomissione(
            Utente utente,
            Hackathon hackathon,
            String nuovoContenuto
    ) {
        /*
         * Il contenuto viene ricontrollato nel momento
         * effettivo dell'aggiornamento.
         */
        verificaContenuto(
                nuovoContenuto
        );

        /*
         * Recuperiamo nuovamente il contesto per evitare
         * di mantenere stato applicativo tra le richieste.
         */
        Sottomissione sottomissione =
                recuperaSottomissione(
                        utente,
                        hackathon
                );

        if (!hackathon.scadenzaSottomissioneNonTrascorsa()) {
            throw new IllegalStateException(
                    "La scadenza per la sottomissione è trascorsa"
            );
        }

        sottomissione.aggiornaContenuto(
                nuovoContenuto
        );

        sottomissioneRepository.salva(
                sottomissione
        );
    }

    private Sottomissione recuperaSottomissione(
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
                teamRepository.recuperaTeam(
                        utenteValido
                ),
                "Il team dell'utente è obbligatorio"
        );

        Partecipazione partecipazione =
                Objects.requireNonNull(
                        partecipazioneRepository
                                .recuperaPartecipazione(
                                        team,
                                        hackathonValido
                                ),
                        "La partecipazione è obbligatoria"
                );

        if (partecipazione.getStato()
                != StatoPartecipazione.ATTIVA) {
            throw new IllegalStateException(
                    "La partecipazione non è attiva"
            );
        }

        return Objects.requireNonNull(
                sottomissioneRepository
                        .recuperaSottomissione(
                                partecipazione
                        ),
                "La sottomissione è obbligatoria"
        );
    }
}