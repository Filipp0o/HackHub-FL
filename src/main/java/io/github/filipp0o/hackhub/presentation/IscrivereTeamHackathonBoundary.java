package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.IscrivereTeamHackathonControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Team;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/iscrizioni")
public class IscrivereTeamHackathonBoundary {

    private final IscrivereTeamHackathonControl
            iscrivereTeamHackathonControl;

    public IscrivereTeamHackathonBoundary(
            IscrivereTeamHackathonControl iscrivereTeamHackathonControl
    ) {
        this.iscrivereTeamHackathonControl =
                Objects.requireNonNull(
                        iscrivereTeamHackathonControl,
                        "Il control di iscrizione è obbligatorio"
                );
    }

    @GetMapping("/hackathons")
    public List<RiepilogoHackathonAperto>
    ottieniHackathonAperti() {

        return iscrivereTeamHackathonControl
                .avviaIscrizione()
                .stream()
                .map(this::creaRiepilogoHackathon)
                .toList();
    }

    @PostMapping("/hackathons/{hackathonId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void iscriviTeam(
            @PathVariable Long hackathonId,
            @RequestBody RichiestaIscrizione richiesta
    ) {
        Long hackathonIdValido =
                Objects.requireNonNull(
                        hackathonId,
                        "L'id dell'hackathon è obbligatorio"
                );

        RichiestaIscrizione richiestaValida =
                Objects.requireNonNull(
                        richiesta,
                        "La richiesta di iscrizione è obbligatoria"
                );

        Utente utente =
                new Utente(
                        richiestaValida.utenteId()
                );

        Hackathon hackathon =
                trovaHackathonAperto(
                        hackathonIdValido
                );

        try {
            Team team =
                    iscrivereTeamHackathonControl
                            .verificaIscrizione(
                                    utente,
                                    hackathon
                            );

            iscrivereTeamHackathonControl
                    .confermaIscrizione(
                            team,
                            hackathon
                    );
        } catch (IllegalStateException eccezione) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    eccezione.getMessage(),
                    eccezione
            );
        }
    }

    private Hackathon trovaHackathonAperto(
            Long hackathonId
    ) {
        return iscrivereTeamHackathonControl
                .avviaIscrizione()
                .stream()
                .filter(hackathon ->
                        Objects.equals(
                                hackathon.getId(),
                                hackathonId
                        )
                )
                .findFirst()
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Hackathon aperto alle iscrizioni non trovato"
                        )
                );
    }

    private RiepilogoHackathonAperto
    creaRiepilogoHackathon(
            Hackathon hackathon
    ) {
        return new RiepilogoHackathonAperto(
                hackathon.getId(),
                hackathon.getNome(),
                hackathon.getScadenzaIscrizioni(),
                hackathon.getDataInizio(),
                hackathon.getDataFine(),
                hackathon.getLuogo(),
                hackathon.getDimensioneMassimaTeam()
        );
    }

    public record RiepilogoHackathonAperto(
            Long id,
            String nome,
            LocalDate scadenzaIscrizioni,
            LocalDate dataInizio,
            LocalDate dataFine,
            String luogo,
            Integer dimensioneMassimaTeam
    ) {
    }

    public record RichiestaIscrizione(
            Long utenteId
    ) {
    }
}