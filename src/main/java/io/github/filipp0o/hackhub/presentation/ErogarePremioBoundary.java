package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ErogarePremioControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Objects;

@Component
public class ErogarePremioBoundary {

    private final ErogarePremioControl erogarePremioControl;

    public ErogarePremioBoundary(
            ErogarePremioControl erogarePremioControl
    ) {
        this.erogarePremioControl = Objects.requireNonNull(
                erogarePremioControl,
                "Il control di erogazione è obbligatorio"
        );
    }

    public RiepilogoErogazione selezionaErogazionePremio(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        erogarePremioControl.avviaErogazionePremio(
                organizzatore,
                hackathon
        );

        Partecipazione partecipazioneVincitrice =
                hackathon.getVincitrice();

        return new RiepilogoErogazione(
                partecipazioneVincitrice
                        .getTeam()
                        .getNome(),
                partecipazioneVincitrice
                        .getTeam()
                        .getResponsabile()
                        .getId(),
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

    public record RiepilogoErogazione(
            String nomeTeamVincitore,
            Long responsabileTeamId,
            BigDecimal importoPremio
    ) {
    }
}