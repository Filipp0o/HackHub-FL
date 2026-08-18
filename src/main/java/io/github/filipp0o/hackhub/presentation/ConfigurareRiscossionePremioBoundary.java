package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ConfigurareRiscossionePremioControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ConfigurareRiscossionePremioBoundary {

    private final ConfigurareRiscossionePremioControl
            configurareRiscossionePremioControl;

    public ConfigurareRiscossionePremioBoundary(
            ConfigurareRiscossionePremioControl
                    configurareRiscossionePremioControl
    ) {
        this.configurareRiscossionePremioControl =
                Objects.requireNonNull(
                        configurareRiscossionePremioControl,
                        "Il control di configurazione è obbligatorio"
                );
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
}