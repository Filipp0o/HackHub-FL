package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ConfigurareRiscossionePremioControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.SistemaPagamentoGateway;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/hackathons/{hackathonId}/premio")
public class ConfigurareRiscossionePremioBoundary {

    private final ConfigurareRiscossionePremioControl
            configurareRiscossionePremioControl;

    private SessioneUtente sessioneUtente;
    private HackathonRepository hackathonRepository;

    public ConfigurareRiscossionePremioBoundary(
            ConfigurareRiscossionePremioControl configurareRiscossionePremioControl
    ) {
        this.configurareRiscossionePremioControl = Objects.requireNonNull(
                configurareRiscossionePremioControl,
                "Il control di configurazione è obbligatorio"
        );
    }

    @Autowired
    public ConfigurareRiscossionePremioBoundary(
            ConfigurareRiscossionePremioControl control,
            SessioneUtente sessioneUtente,
            HackathonRepository hackathonRepository
    ) {
        this(control);
        this.sessioneUtente = Objects.requireNonNull(sessioneUtente);
        this.hackathonRepository = Objects.requireNonNull(hackathonRepository);
    }

    public void selezionaConfigurazioneRiscossionePremio(
            Hackathon hackathon,
            Utente responsabileTeam
    ) {
        configurareRiscossionePremioControl
                .avviaConfigurazioneRiscossionePremio(
                        hackathon,
                        responsabileTeam
                );
    }

    @PostMapping("/configurazione")
    public ResponseEntity<EsitoConfigurazione> configura(
            @PathVariable Long hackathonId
    ) {
        Utente responsabile = sessioneUtente.recupera();
        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        if (hackathon.getVincitrice() != null) {
            SupportoRest.autorizza(
                    hackathon.getVincitrice().getTeam().getResponsabile().getId(),
                    responsabile.getId()
            );
        }

        try {
            SupportoRest.esegui(() -> {
                selezionaConfigurazioneRiscossionePremio(
                        hackathon,
                        responsabile
                );

                return null;
            });

            return ResponseEntity.ok(
                    new EsitoConfigurazione(
                            true,
                            "Riscossione configurata correttamente"
                    )
            );
        } catch (SistemaPagamentoGateway.ConfigurazioneAnnullataException errore) {
            return ResponseEntity.ok(
                    new EsitoConfigurazione(
                            false,
                            "Configurazione annullata"
                    )
            );
        }
    }

    public record EsitoConfigurazione(
            Boolean configurata,
            String messaggio
    ) {
    }
}