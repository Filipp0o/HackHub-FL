package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.AccettareInvitoTeamControl;
import io.github.filipp0o.hackhub.domain.Invito;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/inviti")
public class AccettareInvitoTeamBoundary {

    private final AccettareInvitoTeamControl control;
    private final SessioneUtente sessioneUtente;

    public AccettareInvitoTeamBoundary(
            AccettareInvitoTeamControl control,
            SessioneUtente sessioneUtente
    ) {
        this.control = Objects.requireNonNull(
                control, "Il control è obbligatorio"
        );
        this.sessioneUtente = Objects.requireNonNull(
                sessioneUtente, "La sessione è obbligatoria"
        );
    }

    @GetMapping("/ricevuti")
    public ResponseEntity<EsitoAccettazione> selezionaAccettazioneInvitoTeam() {
        try {
            var inviti = control.richiediInvitiRicevuti(
                    recuperaUtenteAutenticato()
            );
            return inviti.isEmpty()
                    ? mostraNessunInvitoDisponibile()
                    : mostraInvitiRicevuti(inviti);
        } catch (SessioneUtente.UtenteNonAutenticatoException errore) {
            return esito(401, "Accesso richiesto");
        } catch (RuntimeException errore) {
            return esito(500, "Recupero degli inviti non completato");
        }
    }

    @PostMapping("/accettazione")
    public ResponseEntity<EsitoAccettazione> accetta(
            @RequestBody RichiestaAccettazione richiesta
    ) {
        try {
            Utente utente = recuperaUtenteAutenticato();

            if (richiesta == null || richiesta.invitoId() == null
                    || richiesta.invitoId() <= 0) {
                return mostraSelezioneInvitoObbligatoria();
            }

            Invito invito = control.richiediInvitiRicevuti(utente).stream()
                    .filter(candidato ->
                            richiesta.invitoId().equals(candidato.getId()))
                    .findFirst()
                    .orElseThrow(
                            AccettareInvitoTeamControl
                                    .InvitoNonDisponibileException::new
                    );

            control.richiediAccettazioneInvito(utente, invito);
            return mostraAccettazioneCompletata();
        } catch (SessioneUtente.UtenteNonAutenticatoException errore) {
            return esito(401, "Accesso richiesto");
        } catch (AccettareInvitoTeamControl.InvitoNonDisponibileException errore) {
            return esito(404, "Invito non disponibile");
        } catch (AccettareInvitoTeamControl.UtenteGiaInTeamException errore) {
            return mostraUtenteGiaAppartenenteATeam();
        } catch (AccettareInvitoTeamControl.DimensioneMassimaSuperataException errore) {
            return mostraDimensioneMassimaTeamSuperata();
        } catch (RuntimeException errore) {
            return esito(500, "Accettazione non completata");
        }
    }

    public ResponseEntity<EsitoAccettazione> selezionaInvito(Invito invito) {
        return accetta(new RichiestaAccettazione(
                invito == null ? null : invito.getId()
        ));
    }

    public Utente recuperaUtenteAutenticato() {
        return sessioneUtente.recupera();
    }

    public ResponseEntity<EsitoAccettazione> mostraInvitiRicevuti(
            List<Invito> inviti
    ) {
        return ResponseEntity.ok(new EsitoAccettazione(
                "Selezionare un invito",
                inviti.stream()
                        .map(invito -> new InvitoRicevuto(
                                invito.getId(),
                                invito.ottieniTeam().getId(),
                                invito.ottieniTeam().getNome()
                        ))
                        .toList()
        ));
    }

    public ResponseEntity<EsitoAccettazione> mostraNessunInvitoDisponibile() {
        return esito(200, "Nessun invito disponibile");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<EsitoAccettazione> mostraSelezioneInvitoObbligatoria() {
        return esito(400, "È necessario selezionare un invito");
    }

    public ResponseEntity<EsitoAccettazione> mostraUtenteGiaAppartenenteATeam() {
        return esito(409, "L'utente appartiene già a un team");
    }

    public ResponseEntity<EsitoAccettazione> mostraDimensioneMassimaTeamSuperata() {
        return esito(
                409,
                "L'ingresso supera la dimensione massima consentita per il team"
        );
    }

    public ResponseEntity<EsitoAccettazione> mostraAccettazioneCompletata() {
        return esito(200, "Accettazione completata");
    }

    private ResponseEntity<EsitoAccettazione> esito(
            int stato, String messaggio
    ) {
        return ResponseEntity.status(stato).body(
                new EsitoAccettazione(messaggio, List.of())
        );
    }

    public record RichiestaAccettazione(Long invitoId) { }

    public record InvitoRicevuto(Long id, Long teamId, String nomeTeam) { }

    public record EsitoAccettazione(
            String messaggio,
            List<InvitoRicevuto> invitiRicevuti
    ) { }
}