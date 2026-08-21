package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.InviareSottomissioneControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class InviareSottomissioneBoundary {

    private final InviareSottomissioneControl
            inviareSottomissioneControl;

    public InviareSottomissioneBoundary(
            InviareSottomissioneControl inviareSottomissioneControl
    ) {
        this.inviareSottomissioneControl =
                Objects.requireNonNull(
                        inviareSottomissioneControl,
                        "Il control di invio della sottomissione è obbligatorio"
                );
    }

    public Partecipazione selezionaInvioSottomissione(
            Utente utente,
            Hackathon hackathon
    ) {
        return inviareSottomissioneControl
                .avviaInvioSottomissione(
                        utente,
                        hackathon
                );
    }

    public void inserisciContenutoEInvia(
            Partecipazione partecipazione,
            String contenuto
    ) {
        inviareSottomissioneControl
                .verificaContenuto(contenuto);

        inviareSottomissioneControl
                .inviaSottomissione(
                        partecipazione,
                        contenuto
                );
    }
}