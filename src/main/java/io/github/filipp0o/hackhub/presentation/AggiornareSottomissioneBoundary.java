package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.AggiornareSottomissioneControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class AggiornareSottomissioneBoundary {

    private final AggiornareSottomissioneControl
            aggiornareSottomissioneControl;

    public AggiornareSottomissioneBoundary(
            AggiornareSottomissioneControl aggiornareSottomissioneControl
    ) {
        this.aggiornareSottomissioneControl =
                Objects.requireNonNull(
                        aggiornareSottomissioneControl,
                        "Il control di aggiornamento della sottomissione è obbligatorio"
                );
    }

    public String selezionaAggiornamentoSottomissione(
            Utente utente,
            Hackathon hackathon
    ) {
        return aggiornareSottomissioneControl
                .avviaAggiornamentoSottomissione(
                        utente,
                        hackathon
                );
    }

    public void richiediAggiornamento(
            Utente utente,
            Hackathon hackathon,
            String nuovoContenuto
    ) {
        aggiornareSottomissioneControl
                .verificaContenuto(
                        nuovoContenuto
                );

        aggiornareSottomissioneControl
                .aggiornaSottomissione(
                        utente,
                        hackathon,
                        nuovoContenuto
                );
    }
}