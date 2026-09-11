package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ErogarePremioControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Objects;

@RestController
@RequestMapping("/api/hackathons/{hackathonId}/premio")
public class ErogarePremioBoundary {

    private final ErogarePremioControl erogarePremioControl;

    private SessioneUtente sessioneUtente;
    private HackathonRepository hackathonRepository;

    public ErogarePremioBoundary(
            ErogarePremioControl erogarePremioControl
    ) {
        this.erogarePremioControl = Objects.requireNonNull(
                erogarePremioControl,
                "Il control di erogazione è obbligatorio"
        );
    }

    @Autowired
    public ErogarePremioBoundary(
            ErogarePremioControl control,
            SessioneUtente sessioneUtente,
            HackathonRepository hackathonRepository
    ) {
        this(control);
        this.sessioneUtente = Objects.requireNonNull(sessioneUtente);
        this.hackathonRepository = Objects.requireNonNull(hackathonRepository);
    }

    public RiepilogoErogazione selezionaErogazionePremio(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        erogarePremioControl.avviaErogazionePremio(
                organizzatore,
                hackathon
        );

        Partecipazione vincitrice = hackathon.getVincitrice();

        return new RiepilogoErogazione(
                vincitrice.getTeam().getNome(),
                vincitrice.getTeam().getResponsabile().getId(),
                hackathon.getImportoPremio()
        );
    }

    public void confermaErogazionePremio(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        erogarePremioControl.confermaErogazionePremio(
                organizzatore,
                hackathon
        );
    }

    @GetMapping("/erogazione")
    public ResponseEntity<RiepilogoErogazione> ottieniRiepilogo(
            @PathVariable Long hackathonId
    ) {
        Utente organizzatore = sessioneUtente.recupera();
        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        SupportoRest.autorizza(
                hackathon.getOrganizzatore().getId(),
                organizzatore.getId()
        );

        return ResponseEntity.ok(
                SupportoRest.esegui(() ->
                        selezionaErogazionePremio(organizzatore, hackathon)
                )
        );
    }

    @PostMapping("/erogazione")
    public ResponseEntity<Void> eroga(
            @PathVariable Long hackathonId
    ) {
        Utente organizzatore = sessioneUtente.recupera();
        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        SupportoRest.autorizza(
                hackathon.getOrganizzatore().getId(),
                organizzatore.getId()
        );

        SupportoRest.esegui(() -> {
            confermaErogazionePremio(organizzatore, hackathon);
            return null;
        });

        return ResponseEntity.noContent().build();
    }

    public record RiepilogoErogazione(
            String nomeTeamVincitore,
            Long responsabileTeamId,
            BigDecimal importoPremio
    ) {
    }
}