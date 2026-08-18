package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.EsaminareSegnalazioneControl;
import io.github.filipp0o.hackhub.domain.DatiDecisioneSegnalazione;
import io.github.filipp0o.hackhub.domain.EsitoSegnalazione;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/segnalazioni")
public class EsaminareSegnalazioneBoundary {

    private final EsaminareSegnalazioneControl
            esaminareSegnalazioneControl;

    public EsaminareSegnalazioneBoundary(
            EsaminareSegnalazioneControl
                    esaminareSegnalazioneControl
    ) {
        this.esaminareSegnalazioneControl =
                Objects.requireNonNull(
                        esaminareSegnalazioneControl,
                        "Il control di esame è obbligatorio"
                );
    }

    @GetMapping("/da-esaminare")
    public List<RiepilogoSegnalazione>
    ottieniSegnalazioniDaEsaminare(
            @RequestParam Long organizzatoreId
    ) {
        Utente organizzatore = new Utente(
                organizzatoreId
        );

        return esaminareSegnalazioneControl
                .avviaEsameSegnalazioni(organizzatore)
                .stream()
                .map(this::creaRiepilogo)
                .toList();
    }

    @GetMapping("/{segnalazioneId}")
    public RiepilogoSegnalazione selezionaSegnalazione(
            @PathVariable Long segnalazioneId,
            @RequestParam Long organizzatoreId
    ) {
        Utente organizzatore = new Utente(
                organizzatoreId
        );

        Segnalazione segnalazione = trovaSegnalazione(
                segnalazioneId,
                organizzatore
        );

        return creaRiepilogo(
                esaminareSegnalazioneControl
                        .selezionaSegnalazione(
                                segnalazione,
                                organizzatore
                        )
        );
    }

    public RiepilogoSegnalazione
    selezionaNotificaSegnalazione(
            NotificaSegnalazione notificaSegnalazione,
            Utente organizzatore
    ) {
        return creaRiepilogo(
                esaminareSegnalazioneControl
                        .apriSegnalazioneDaNotifica(
                                notificaSegnalazione,
                                organizzatore
                        )
        );
    }

    @PostMapping("/{segnalazioneId}/decisione")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registraDecisione(
            @PathVariable Long segnalazioneId,
            @RequestBody RichiestaDecisione richiesta
    ) {
        RichiestaDecisione richiestaValida =
                Objects.requireNonNull(
                        richiesta,
                        "La decisione è obbligatoria"
                );

        Utente organizzatore = new Utente(
                richiestaValida.organizzatoreId()
        );

        Segnalazione segnalazione = trovaSegnalazione(
                segnalazioneId,
                organizzatore
        );

        esaminareSegnalazioneControl
                .selezionaSegnalazione(
                        segnalazione,
                        organizzatore
                );

        DatiDecisioneSegnalazione dati =
                new DatiDecisioneSegnalazione(
                        richiestaValida.esito(),
                        richiestaValida.motivazione()
                );

        esaminareSegnalazioneControl
                .verificaDecisione(dati);

        esaminareSegnalazioneControl
                .registraDecisione(
                        segnalazione,
                        organizzatore,
                        dati
                );
    }

    private Segnalazione trovaSegnalazione(
            Long segnalazioneId,
            Utente organizzatore
    ) {
        return esaminareSegnalazioneControl
                .avviaEsameSegnalazioni(organizzatore)
                .stream()
                .filter(segnalazione -> Objects.equals(
                        segnalazione.getId(),
                        segnalazioneId
                ))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Segnalazione non trovata"
                ));
    }

    private RiepilogoSegnalazione creaRiepilogo(
            Segnalazione segnalazione
    ) {
        return new RiepilogoSegnalazione(
                segnalazione.getId(),
                segnalazione.getDescrizione(),
                segnalazione
                        .getPartecipazione()
                        .getTeam()
                        .getNome(),
                segnalazione
                        .getPartecipazione()
                        .getHackathon()
                        .getRegolamento(),
                List.of(EsitoSegnalazione.values())
        );
    }

    public record RiepilogoSegnalazione(
            Long id,
            String descrizione,
            String nomeTeam,
            String regolamento,
            List<EsitoSegnalazione> esitiDisponibili
    ) {
    }

    public record RichiestaDecisione(
            Long organizzatoreId,
            EsitoSegnalazione esito,
            String motivazione
    ) {
    }
}