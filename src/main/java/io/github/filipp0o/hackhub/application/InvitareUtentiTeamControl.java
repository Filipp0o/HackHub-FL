package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;
import java.util.Objects;

public class InvitareUtentiTeamControl {

    private final UtenteRepository utenteRepository;
    private final TeamRepository teamRepository;
    private final InvitoRepository invitoRepository;

    public InvitareUtentiTeamControl(
            UtenteRepository utenteRepository,
            TeamRepository teamRepository,
            InvitoRepository invitoRepository
    ) {
        this.utenteRepository = Objects.requireNonNull(
                utenteRepository, "Il repository degli utenti è obbligatorio"
        );
        this.teamRepository = Objects.requireNonNull(
                teamRepository, "Il repository dei team è obbligatorio"
        );
        this.invitoRepository = Objects.requireNonNull(
                invitoRepository, "Il repository degli inviti è obbligatorio"
        );
    }

    public List<Utente> richiediUtentiInvitabili(Utente utente) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");
        return utenteRepository.recuperaUtentiInvitabili(utente);
    }

    public void richiediInvito(Utente utente, Utente utenteInvitato) {
        Objects.requireNonNull(utente, "L'utente è obbligatorio");
        if (utenteInvitato == null || utenteInvitato.getId() == null) {
            throw new UtenteNonInvitabileException();
        }

        Team team = teamRepository.recuperaTeamCreatoDa(utente);
        Utente destinatario = utenteRepository.recuperaUtentiInvitabili(utente).stream()
                .filter(candidato -> utenteInvitato.getId().equals(candidato.getId()))
                .findFirst()
                .orElseThrow(UtenteNonInvitabileException::new);

        Invito invito = Invito.crea(team, destinatario);
        invitoRepository.salva(invito);
    }

    public static class UtenteNonInvitabileException
            extends IllegalArgumentException {

        public UtenteNonInvitabileException() {
            super("L'utente selezionato non è invitabile");
        }
    }
}