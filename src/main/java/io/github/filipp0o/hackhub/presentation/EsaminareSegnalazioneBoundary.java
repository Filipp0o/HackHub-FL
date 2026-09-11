package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.EsaminareSegnalazioneControl;
import io.github.filipp0o.hackhub.domain.DatiDecisioneSegnalazione;
import io.github.filipp0o.hackhub.domain.EsitoSegnalazione;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/segnalazioni")
public class EsaminareSegnalazioneBoundary {

    private final EsaminareSegnalazioneControl esaminareSegnalazioneControl;
    private final SessioneUtente sessioneUtente;

    public EsaminareSegnalazioneBoundary(
            EsaminareSegnalazioneControl esaminareSegnalazioneControl,
            SessioneUtente sessioneUtente
    ) {
        this.esaminareSegnalazioneControl = Objects.requireNonNull(
                esaminareSegnalazioneControl,
                "Il control di esame è obbligatorio"
        );
        this.sessioneUtente = Objects.requireNonNull(
                sessioneUtente,
                "La sessione utente è obbligatoria"
        );
    }

    @GetMapping("/da-esaminare")
    public List<RiepilogoSegnalazione> ottieniSegnalazioniDaEsaminare() {
        Utente organizzatore = sessioneUtente.recupera();

        return esaminareSegnalazioneControl
                .avviaEsameSegnalazioni(organizzatore)
                .stream()
                .map(this::creaRiepilogo)
                .toList();
    }

    @GetMapping("/{segnalazioneId}")
    public RiepilogoSegnalazione selezionaSegnalazione(
            @PathVariable Long segnalazioneId
    ) {
        Utente organizzatore = sessioneUtente.recupera();

        Segnalazione segnalazione = trovaSegnalazione(
                segnalazioneId,
                organizzatore
        );

        return creaRiepilogo(
                esaminareSegnalazioneControl.selezionaSegnalazione(
                        segnalazione,
                        organizzatore
                )
        );
    }

    public RiepilogoSegnalazione selezionaNotificaSegnalazione(
            NotificaSegnalazione notificaSegnalazione
    ) {
        return creaRiepilogo(
                esaminareSegnalazioneControl.apriSegnalazioneDaNotifica(
                        notificaSegnalazione,
                        sessioneUtente.recupera()
                )
        );
    }

    @GetMapping("/notifiche")
    public List<RiepilogoNotifica> ottieniNotificheRicevute() {
        Utente organizzatore = sessioneUtente.recupera();

        return SupportoRest.leggi(() ->
                        esaminareSegnalazioneControl
                                .ottieniNotificheRicevute(organizzatore)
                )
                .stream()
                .map(n -> new RiepilogoNotifica(
                        n.getId(),
                        n.getSegnalazione().getId(),
                        n.getLetta()
                ))
                .toList();
    }

    @PostMapping("/notifiche/{notificaId}/apertura")
    public RiepilogoSegnalazione apriNotificaSegnalazione(
            @PathVariable Long notificaId
    ) {
        Utente organizzatore = sessioneUtente.recupera();
        SupportoRest.id(notificaId);

        NotificaSegnalazione notifica = SupportoRest.leggi(() ->
                        esaminareSegnalazioneControl
                                .ottieniNotificheRicevute(organizzatore)
                )
                .stream()
                .filter(n -> Objects.equals(n.getId(), notificaId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Notifica non trovata"
                ));

        return creaRiepilogo(
                SupportoRest.esegui(() ->
                        esaminareSegnalazioneControl.apriSegnalazioneDaNotifica(
                                notifica,
                                organizzatore
                        )
                )
        );
    }

    @PostMapping("/{segnalazioneId}/decisione/verifica")
    public RiepilogoDecisione verificaDecisione(
            @PathVariable Long segnalazioneId,
            @RequestBody RichiestaDecisione richiesta
    ) {
        Utente organizzatore = sessioneUtente.recupera();
        RichiestaDecisione valida = validaRichiesta(richiesta);

        Segnalazione segnalazione = trovaSegnalazione(
                segnalazioneId,
                organizzatore
        );

        SupportoRest.esegui(() ->
                esaminareSegnalazioneControl.selezionaSegnalazione(
                        segnalazione,
                        organizzatore
                )
        );

        esaminareSegnalazioneControl.verificaDecisione(
                new DatiDecisioneSegnalazione(
                        valida.esito(),
                        valida.motivazione()
                )
        );

        return new RiepilogoDecisione(
                segnalazioneId,
                valida.esito(),
                valida.motivazione()
        );
    }

    @PostMapping("/{segnalazioneId}/decisione")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void registraDecisione(
            @PathVariable Long segnalazioneId,
            @RequestBody RichiestaDecisione richiesta
    ) {
        Utente organizzatore = sessioneUtente.recupera();
        RichiestaDecisione valida = validaRichiesta(richiesta);

        Segnalazione segnalazione = trovaSegnalazione(
                segnalazioneId,
                organizzatore
        );

        esaminareSegnalazioneControl.selezionaSegnalazione(
                segnalazione,
                organizzatore
        );

        DatiDecisioneSegnalazione dati = new DatiDecisioneSegnalazione(
                valida.esito(),
                valida.motivazione()
        );

        esaminareSegnalazioneControl.verificaDecisione(dati);

        SupportoRest.esegui(() -> {
            esaminareSegnalazioneControl.registraDecisione(
                    segnalazione,
                    organizzatore,
                    dati
            );

            return null;
        });
    }

    private RichiestaDecisione validaRichiesta(
            RichiestaDecisione richiesta
    ) {
        SupportoRest.richiesta(richiesta);

        if (richiesta.esito() == null) {
            throw new IllegalArgumentException(
                    "L'esito della segnalazione è obbligatorio"
            );
        }

        return richiesta;
    }

    private Segnalazione trovaSegnalazione(
            Long segnalazioneId,
            Utente organizzatore
    ) {
        return esaminareSegnalazioneControl
                .avviaEsameSegnalazioni(organizzatore)
                .stream()
                .filter(s -> Objects.equals(s.getId(), segnalazioneId))
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
                segnalazione.getPartecipazione().getTeam().getNome(),
                segnalazione.getPartecipazione().getHackathon().getRegolamento(),
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
            EsitoSegnalazione esito,
            String motivazione
    ) {
    }

    public record RiepilogoNotifica(
            Long id,
            Long segnalazioneId,
            Boolean letta
    ) {
    }

    public record RiepilogoDecisione(
            Long segnalazioneId,
            EsitoSegnalazione esito,
            String motivazione
    ) {
    }
}