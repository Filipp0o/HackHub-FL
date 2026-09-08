package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.Objects;

public class CreareTeamControl {

    private final TeamRepository teamRepository;

    public CreareTeamControl(TeamRepository teamRepository) {
        this.teamRepository = Objects.requireNonNull(
                teamRepository,
                "Il repository dei team è obbligatorio"
        );
    }

    public void avviaCreazioneTeam(Utente utente) {
        Objects.requireNonNull(
                utente,
                "L'utente è obbligatorio"
        );

        if (teamRepository.verificaAppartenenzaTeam(utente)) {
            throw new UtenteGiaInTeamException();
        }
    }

    public void verificaNomeTeam(String nome) {
        if (nome == null || nome.isBlank()) {
            throw new IllegalArgumentException(
                    "Il nome del team è obbligatorio"
            );
        }
    }

    public void creaTeam(String nome, Utente utente) {
        Team team = Team.crea(nome, utente, utente);

        try {
            teamRepository.salva(team);
        } catch (RuntimeException errore) {
            throw new CreazioneTeamFallitaException(errore);
        }
    }

    public static class UtenteGiaInTeamException
            extends IllegalStateException {

        public UtenteGiaInTeamException() {
            super("L'utente appartiene già a un team");
        }
    }

    public static class CreazioneTeamFallitaException
            extends IllegalStateException {

        public CreazioneTeamFallitaException(Throwable causa) {
            super("Il team non è stato creato", causa);
        }
    }
}