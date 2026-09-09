package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareTeamControl;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Objects;

@RestController
@RequestMapping("/api/teams")
public class CreareTeamBoundary {

    private final CreareTeamControl creareTeamControl;
    private final SessioneUtente sessioneUtente;

    public CreareTeamBoundary(
            CreareTeamControl creareTeamControl,
            SessioneUtente sessioneUtente
    ) {
        this.creareTeamControl = Objects.requireNonNull(
                creareTeamControl,
                "Il control di creazione del team è obbligatorio"
        );
        this.sessioneUtente = Objects.requireNonNull(
                sessioneUtente,
                "La sessione utente è obbligatoria"
        );
    }

    @GetMapping("/creazione")
    public EsitoAvvioCreazioneTeam avviaCreazioneTeam() {
        Utente utente = sessioneUtente.recupera();
        creareTeamControl.avviaCreazioneTeam(utente);

        return new EsitoAvvioCreazioneTeam(
                "Inserire il nome del team: ne diventerai automaticamente membro e responsabile",
                utente.getId()
        );
    }

    @PostMapping("/verifica")
    public RiepilogoCreazioneTeam verificaCreazioneTeam(
            @RequestBody RichiestaCreazioneTeam richiesta
    ) {
        Utente utente = sessioneUtente.recupera();
        Objects.requireNonNull(
                richiesta,
                "La richiesta di creazione è obbligatoria"
        );
        creareTeamControl.verificaNomeTeam(richiesta.nome());

        return new RiepilogoCreazioneTeam(richiesta.nome(), utente.getId());
    }

    public record EsitoAvvioCreazioneTeam(
            String messaggio,
            Long responsabileId
    ) {
    }

    public record RiepilogoCreazioneTeam(
            String nome,
            Long responsabileId
    ) {
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public void creaTeam(
            @RequestBody RichiestaCreazioneTeam richiesta
    ) {
        RichiestaCreazioneTeam richiestaValida = Objects.requireNonNull(
                richiesta,
                "La richiesta di creazione è obbligatoria"
        );

        Utente utente = sessioneUtente.recupera();

        creareTeamControl.avviaCreazioneTeam(utente);
        creareTeamControl.verificaNomeTeam(richiestaValida.nome());
        creareTeamControl.creaTeam(richiestaValida.nome(), utente);
    }

    public record RichiestaCreazioneTeam(
            String nome
    ) {
    }
}