package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.application.InviareSottomissioneControl;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/hackathons/{hackathonId}/sottomissione")
public class InviareSottomissioneBoundary {

    private final InviareSottomissioneControl inviareSottomissioneControl;

    private SessioneUtente sessioneUtente;
    private HackathonRepository hackathonRepository;

    public InviareSottomissioneBoundary(
            InviareSottomissioneControl inviareSottomissioneControl
    ) {
        this.inviareSottomissioneControl = Objects.requireNonNull(
                inviareSottomissioneControl,
                "Il control di invio della sottomissione è obbligatorio"
        );
    }

    @Autowired
    public InviareSottomissioneBoundary(
            InviareSottomissioneControl control,
            SessioneUtente sessioneUtente,
            HackathonRepository hackathonRepository
    ) {
        this(control);
        this.sessioneUtente = Objects.requireNonNull(sessioneUtente);
        this.hackathonRepository = Objects.requireNonNull(hackathonRepository);
    }

    public Partecipazione selezionaInvioSottomissione(
            Utente utente,
            Hackathon hackathon
    ) {
        return inviareSottomissioneControl.avviaInvioSottomissione(
                utente,
                hackathon
        );
    }

    public void inserisciContenutoEInvia(
            Partecipazione partecipazione,
            String contenuto
    ) {
        inviareSottomissioneControl.verificaContenuto(contenuto);
        inviareSottomissioneControl.inviaSottomissione(
                partecipazione,
                contenuto
        );
    }

    @GetMapping("/invio")
    public ResponseEntity<DatiInvioSottomissione> avviaInvio(
            @PathVariable Long hackathonId
    ) {
        Utente utente = sessioneUtente.recupera();
        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        SupportoRest.esegui(() ->
                selezionaInvioSottomissione(utente, hackathon)
        );

        return ResponseEntity.ok(
                new DatiInvioSottomissione(
                        "Inserire il contenuto della sottomissione"
                )
        );
    }

    @PostMapping
    public ResponseEntity<Void> invia(
            @PathVariable Long hackathonId,
            @RequestBody RichiestaSottomissione richiesta
    ) {
        Utente utente = sessioneUtente.recupera();

        SupportoRest.richiesta(richiesta);
        inviareSottomissioneControl.verificaContenuto(richiesta.contenuto());

        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        SupportoRest.esegui(() -> {
            Partecipazione partecipazione =
                    selezionaInvioSottomissione(utente, hackathon);

            inserisciContenutoEInvia(
                    partecipazione,
                    richiesta.contenuto()
            );

            return null;
        });

        return ResponseEntity.status(201).build();
    }

    public record DatiInvioSottomissione(String messaggio) {
    }
}