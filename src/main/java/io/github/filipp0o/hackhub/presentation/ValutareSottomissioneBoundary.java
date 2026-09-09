package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl;
import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl.RegistrazioneValutazioneFallitaException;
import io.github.filipp0o.hackhub.domain.DatiValutazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Sottomissione;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/valutazioni")
public class ValutareSottomissioneBoundary {

    private final ValutareSottomissioneControl
            valutareSottomissioneControl;

    private final SessioneUtente sessioneUtente;

    public ValutareSottomissioneBoundary(
            ValutareSottomissioneControl
                    valutareSottomissioneControl,
            SessioneUtente sessioneUtente
    ) {
        this.valutareSottomissioneControl =
                Objects.requireNonNull(
                        valutareSottomissioneControl,
                        "Il control di valutazione è obbligatorio"
                );

        this.sessioneUtente = Objects.requireNonNull(
                sessioneUtente,
                "La sessione utente è obbligatoria"
        );
    }

    @GetMapping("/hackathons")
    public List<RiepilogoHackathon> ottieniHackathonValutabili() {
        Utente giudice = sessioneUtente.recupera();

        return valutareSottomissioneControl
                .avviaValutazioneSottomissione(giudice)
                .stream()
                .map(hackathon -> new RiepilogoHackathon(
                        hackathon.getId(),
                        hackathon.getNome()
                ))
                .toList();
    }

    @GetMapping(
            "/hackathons/{hackathonId}/sottomissioni"
    )
    public List<RiepilogoSottomissione>
    ottieniSottomissioniDaValutare(
            @PathVariable Long hackathonId
    ) {
        Utente giudice = sessioneUtente.recupera();

        Hackathon hackathon = trovaHackathonValutabile(
                hackathonId,
                giudice
        );

        try {
            return valutareSottomissioneControl
                    .selezionaHackathon(hackathon)
                    .stream()
                    .map(sottomissione ->
                            new RiepilogoSottomissione(
                                    sottomissione.getId(),
                                    sottomissione.getContenuto(),
                                    hackathon.getCriteriValutazione()
                            )
                    )
                    .toList();
        } catch (IllegalStateException errore) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    errore.getMessage(),
                    errore
            );
        }
    }

    @PostMapping(
            "/hackathons/{hackathonId}/sottomissioni/{sottomissioneId}"
    )
    @ResponseStatus(HttpStatus.CREATED)
    public void registraValutazione(
            @PathVariable Long hackathonId,
            @PathVariable Long sottomissioneId,
            @RequestBody RichiestaValutazione richiesta
    ) {
        RichiestaValutazione richiestaValida =
                Objects.requireNonNull(
                        richiesta,
                        "La richiesta di valutazione è obbligatoria"
                );

        Utente giudice = sessioneUtente.recupera();

        Hackathon hackathon = trovaHackathonValutabile(
                hackathonId,
                giudice
        );

        try {
            Sottomissione sottomissione =
                    trovaSottomissioneDaValutare(
                            hackathon,
                            sottomissioneId
                    );

            DatiValutazione dati = new DatiValutazione(
                    richiestaValida.giudizio(),
                    richiestaValida.punteggio()
            );

            valutareSottomissioneControl
                    .selezionaSottomissione(sottomissione);

            valutareSottomissioneControl
                    .verificaDatiValutazione(dati);

            valutareSottomissioneControl
                    .confermaValutazione(
                            sottomissione,
                            giudice,
                            dati
                    );
        } catch (RegistrazioneValutazioneFallitaException errore) {
            throw errore;
        } catch (IllegalStateException errore) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    errore.getMessage(),
                    errore
            );
        }
    }

    private Hackathon trovaHackathonValutabile(
            Long hackathonId,
            Utente giudice
    ) {
        return valutareSottomissioneControl
                .avviaValutazioneSottomissione(giudice)
                .stream()
                .filter(hackathon -> Objects.equals(
                        hackathon.getId(),
                        hackathonId
                ))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hackathon non trovato"
                ));
    }

    private Sottomissione trovaSottomissioneDaValutare(
            Hackathon hackathon,
            Long sottomissioneId
    ) {
        return valutareSottomissioneControl
                .selezionaHackathon(hackathon)
                .stream()
                .filter(sottomissione -> Objects.equals(
                        sottomissione.getId(),
                        sottomissioneId
                ))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Sottomissione non trovata"
                ));
    }

    public record RiepilogoHackathon(
            Long id,
            String nome
    ) {
    }

    public record RiepilogoSottomissione(
            Long id,
            String contenuto,
            String criteriValutazione
    ) {
    }

    public record RichiestaValutazione(
            String giudizio,
            BigDecimal punteggio
    ) {
    }
}