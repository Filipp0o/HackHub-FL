package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.AggiornareSottomissioneControl;
import io.github.filipp0o.hackhub.application.HackathonRepository;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/hackathons/{hackathonId}/sottomissione")
public class AggiornareSottomissioneBoundary {

    private final AggiornareSottomissioneControl aggiornareSottomissioneControl;

    private SessioneUtente sessioneUtente;
    private HackathonRepository hackathonRepository;

    public AggiornareSottomissioneBoundary(
            AggiornareSottomissioneControl aggiornareSottomissioneControl
    ) {
        this.aggiornareSottomissioneControl = Objects.requireNonNull(
                aggiornareSottomissioneControl,
                "Il control di aggiornamento della sottomissione è obbligatorio"
        );
    }

    @Autowired
    public AggiornareSottomissioneBoundary(
            AggiornareSottomissioneControl control,
            SessioneUtente sessioneUtente,
            HackathonRepository hackathonRepository
    ) {
        this(control);
        this.sessioneUtente = Objects.requireNonNull(sessioneUtente);
        this.hackathonRepository = Objects.requireNonNull(hackathonRepository);
    }

    public String selezionaAggiornamentoSottomissione(
            Utente utente,
            Hackathon hackathon
    ) {
        return aggiornareSottomissioneControl.avviaAggiornamentoSottomissione(
                utente,
                hackathon
        );
    }

    public void richiediAggiornamento(
            Utente utente,
            Hackathon hackathon,
            String nuovoContenuto
    ) {
        aggiornareSottomissioneControl.verificaContenuto(nuovoContenuto);
        aggiornareSottomissioneControl.aggiornaSottomissione(
                utente,
                hackathon,
                nuovoContenuto
        );
    }

    @GetMapping
    public ResponseEntity<ContenutoSottomissione> ottieniContenuto(
            @PathVariable Long hackathonId
    ) {
        Utente utente = sessioneUtente.recupera();
        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        String contenuto = SupportoRest.esegui(() ->
                selezionaAggiornamentoSottomissione(utente, hackathon)
        );

        return ResponseEntity.ok(new ContenutoSottomissione(contenuto));
    }

    @PutMapping
    public ResponseEntity<Void> aggiorna(
            @PathVariable Long hackathonId,
            @RequestBody RichiestaSottomissione richiesta
    ) {
        Utente utente = sessioneUtente.recupera();

        SupportoRest.richiesta(richiesta);
        aggiornareSottomissioneControl.verificaContenuto(
                richiesta.contenuto()
        );

        Hackathon hackathon =
                SupportoRest.hackathon(hackathonRepository, hackathonId);

        SupportoRest.esegui(() -> {
            richiediAggiornamento(
                    utente,
                    hackathon,
                    richiesta.contenuto()
            );

            return null;
        });

        return ResponseEntity.noContent().build();
    }

    public record ContenutoSottomissione(String contenuto) {
    }
}