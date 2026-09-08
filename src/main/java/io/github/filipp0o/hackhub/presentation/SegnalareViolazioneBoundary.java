package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.SegnalareViolazioneControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/segnalazioni")
public class SegnalareViolazioneBoundary {

    private final SegnalareViolazioneControl segnalareViolazioneControl;

    private final SessioneUtente sessioneUtente;

    public SegnalareViolazioneBoundary(
            SegnalareViolazioneControl segnalareViolazioneControl,
            SessioneUtente sessioneUtente
    ) {
        this.segnalareViolazioneControl = Objects.requireNonNull(
                segnalareViolazioneControl,
                "Il control di segnalazione è obbligatorio"
        );

        this.sessioneUtente = Objects.requireNonNull(
                sessioneUtente,
                "La sessione utente è obbligatoria"
        );
    }

    @GetMapping("/hackathons")
    public List<RiepilogoHackathon> ottieniHackathonSegnalabili() {
        Utente mentore = sessioneUtente.recupera();

        return segnalareViolazioneControl
                .avviaSegnalazioneViolazione(mentore)
                .stream()
                .map(hackathon -> new RiepilogoHackathon(
                        hackathon.getId(),
                        hackathon.getNome()
                ))
                .toList();
    }

    @GetMapping("/hackathons/{hackathonId}/partecipazioni")
    public List<RiepilogoPartecipazione> ottieniPartecipazioniSegnalabili(
            @PathVariable Long hackathonId
    ) {
        Utente mentore = sessioneUtente.recupera();

        Hackathon hackathon = trovaHackathonSegnalabile(
                hackathonId,
                mentore
        );

        return segnalareViolazioneControl
                .selezionaHackathon(hackathon)
                .stream()
                .map(this::creaRiepilogoPartecipazione)
                .toList();
    }

    @PostMapping(
            "/hackathons/{hackathonId}/partecipazioni/{partecipazioneId}"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public void registraSegnalazione(
            @PathVariable Long hackathonId,
            @PathVariable Long partecipazioneId,
            @RequestBody RichiestaSegnalazione richiesta
    ) {
        RichiestaSegnalazione richiestaValida =
                Objects.requireNonNull(
                        richiesta,
                        "La richiesta di segnalazione è obbligatoria"
                );

        Utente mentore = sessioneUtente.recupera();

        Hackathon hackathon = trovaHackathonSegnalabile(
                hackathonId,
                mentore
        );

        Partecipazione partecipazione = trovaPartecipazione(
                hackathon,
                partecipazioneId
        );

        segnalareViolazioneControl.selezionaTeam(
                partecipazione
        );

        segnalareViolazioneControl.verificaDescrizione(
                richiestaValida.descrizione()
        );

        segnalareViolazioneControl
                .registraSegnalazioneConNotifica(
                        mentore,
                        partecipazione,
                        richiestaValida.descrizione()
                );
    }

    private Hackathon trovaHackathonSegnalabile(
            Long hackathonId,
            Utente mentore
    ) {
        return segnalareViolazioneControl
                .avviaSegnalazioneViolazione(mentore)
                .stream()
                .filter(hackathon -> Objects.equals(
                        hackathon.getId(),
                        hackathonId
                ))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hackathon non trovato"
                ));
    }

    private Partecipazione trovaPartecipazione(
            Hackathon hackathon,
            Long partecipazioneId
    ) {
        return segnalareViolazioneControl
                .selezionaHackathon(hackathon)
                .stream()
                .filter(partecipazione -> Objects.equals(
                        partecipazione.getId(),
                        partecipazioneId
                ))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Partecipazione non trovata"
                ));
    }

    private RiepilogoPartecipazione creaRiepilogoPartecipazione(
            Partecipazione partecipazione
    ) {
        return new RiepilogoPartecipazione(
                partecipazione.getId(),
                partecipazione.getTeam().getNome(),
                partecipazione
                        .getTeam()
                        .getResponsabile()
                        .getId(),
                segnalareViolazioneControl
                        .selezionaTeam(partecipazione)
        );
    }

    public record RiepilogoHackathon(
            Long id,
            String nome
    ) {
    }

    public record RiepilogoPartecipazione(
            Long id,
            String nomeTeam,
            Long responsabileId,
            String regolamento
    ) {
    }

    public record RichiestaSegnalazione(
            String descrizione
    ) {
    }
}