package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.IscrivereTeamHackathonControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
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

    private final IscrivereTeamHackathonControl iscrivereTeamHackathonControl;
    private final SessioneUtente sessioneUtente;

    public IscrivereTeamHackathonBoundary(
            IscrivereTeamHackathonControl iscrivereTeamHackathonControl,
            SessioneUtente sessioneUtente
    ) {
        this.iscrivereTeamHackathonControl = Objects.requireNonNull(
                iscrivereTeamHackathonControl,
                "Il control di iscrizione è obbligatorio"
        );
        this.sessioneUtente = Objects.requireNonNull(
                sessioneUtente,
                "La sessione utente è obbligatoria"
        );
    }

    @GetMapping("/hackathons")
    public List<RiepilogoHackathonAperto> ottieniHackathonAperti() {
        sessioneUtente.recupera();

        return iscrivereTeamHackathonControl
                .avviaIscrizione()
                .stream()
                .map(this::creaRiepilogoHackathon)
                .toList();
    }

    @GetMapping("/hackathons/{hackathonId}/riepilogo")
    public RiepilogoIscrizione preparaIscrizione(
            @PathVariable Long hackathonId
    ) {
        Utente utente = sessioneUtente.recupera();
        Hackathon hackathon = trovaHackathonAperto(hackathonId);

        try {
            Team team = iscrivereTeamHackathonControl.verificaIscrizione(
                    utente, hackathon
            );

            return new RiepilogoIscrizione(
                    team.getId(),
                    team.getNome(),
                    creaRiepilogoHackathon(hackathon)
            );
        } catch (IllegalStateException errore) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    errore.getMessage(),
                    errore
            );
        }
    }

    public record RiepilogoIscrizione(
            Long teamId,
            String nomeTeam,
            RiepilogoHackathonAperto hackathon
    ) {
    }

    @PostMapping("/hackathons/{hackathonId}")
    @ResponseStatus(HttpStatus.CREATED)
    public void iscriviTeam(
            @PathVariable Long hackathonId
    ) {
        Long hackathonIdValido = Objects.requireNonNull(
                hackathonId,
                "L'id dell'hackathon è obbligatorio"
        );

        Utente utente = sessioneUtente.recupera();
        Hackathon hackathon = trovaHackathonAperto(hackathonIdValido);

        try {
            Team team = iscrivereTeamHackathonControl.verificaIscrizione(
                    utente, hackathon
            );

            iscrivereTeamHackathonControl.confermaIscrizione(team, hackathon);
        } catch (IllegalStateException eccezione) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    eccezione.getMessage(),
                    eccezione
            );
        }
    }

    private Hackathon trovaHackathonAperto(Long hackathonId) {
        return iscrivereTeamHackathonControl
                .avviaIscrizione()
                .stream()
                .filter(hackathon -> Objects.equals(
                        hackathon.getId(), hackathonId
                ))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hackathon aperto alle iscrizioni non trovato"
                ));
    }

    private RiepilogoHackathonAperto creaRiepilogoHackathon(
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
}