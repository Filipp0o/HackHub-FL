package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

public class AccettareInvitoTeamControl {

    private final InvitoRepository invitoRepository;
    private final TeamRepository teamRepository;
    private final PartecipazioneRepository partecipazioneRepository;

    public AccettareInvitoTeamControl(
            InvitoRepository invitoRepository,
            TeamRepository teamRepository,
            PartecipazioneRepository partecipazioneRepository
    ) {
        this.invitoRepository = Objects.requireNonNull(
                invitoRepository,
                "Il repository degli inviti è obbligatorio"
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

    public List<Invito> richiediInvitiRicevuti(Utente utente) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");
        return invitoRepository.recuperaInvitiRicevuti(utente);
    }

    // Serializza il corpo del metodo nella stessa istanza;
    // non sostituisce i lock sul database.
    @Transactional
    public synchronized void richiediAccettazioneInvito(
            Utente utente, Invito invito
    ) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");

        if (invito == null || invito.getId() == null) {
            throw new InvitoNonDisponibileException();
        }

        Invito ricevuto = richiediInvitiRicevuti(utente).stream()
                .filter(candidato ->
                        invito.getId().equals(candidato.getId()))
                .findFirst()
                .orElseThrow(InvitoNonDisponibileException::new);

        if (teamRepository.verificaAppartenenzaTeam(utente)) {
            throw new UtenteGiaInTeamException();
        }

        Team team = ricevuto.ottieniTeam();
        verificaAmmissibilitaNuovoMembro(team);

        ricevuto.registraAccettazione();
        team.aggiungiMembro(utente);

        invitoRepository.salva(ricevuto);
        teamRepository.salva(team);
    }

    public void verificaAmmissibilitaNuovoMembro(Team team) {
        Objects.requireNonNull(team, "Il team è obbligatorio");

        int nuovaDimensione = Math.addExact(team.numeroMembri(), 1);

        var partecipazioni = partecipazioneRepository
                .recuperaPartecipazioniInHackathonNonConclusi(team);

        for (var partecipazione : partecipazioni) {
            if (!partecipazione.ottieniHackathon()
                    .rispettaDimensioneMassima(nuovaDimensione)) {
                throw new DimensioneMassimaSuperataException();
            }
        }
    }

    public static class InvitoNonDisponibileException
            extends IllegalArgumentException {

        public InvitoNonDisponibileException() {
            super("L'invito selezionato non è disponibile per l'utente");
        }
    }

    public static class UtenteGiaInTeamException
            extends IllegalStateException {

        public UtenteGiaInTeamException() {
            super("L'utente appartiene già a un team");
        }
    }

    public static class DimensioneMassimaSuperataException
            extends IllegalStateException {

        public DimensioneMassimaSuperataException() {
            super(
                    "L'ingresso supera la dimensione massima consentita per il team"
            );
        }
    }
}