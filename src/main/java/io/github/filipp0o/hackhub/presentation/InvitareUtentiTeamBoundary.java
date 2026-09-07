package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.InvitareUtentiTeamControl;
import io.github.filipp0o.hackhub.application.TeamRepository;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RestController
@RequestMapping("/api/inviti")
public class InvitareUtentiTeamBoundary {

    private final InvitareUtentiTeamControl control;
    private final SessioneUtente sessioneUtente;

    public InvitareUtentiTeamBoundary(
            InvitareUtentiTeamControl control, SessioneUtente sessioneUtente
    ) {
        this.control = Objects.requireNonNull(control, "Il control è obbligatorio");
        this.sessioneUtente = Objects.requireNonNull(sessioneUtente, "La sessione è obbligatoria");
    }

    @GetMapping("/utenti-invitabili")
    public ResponseEntity<EsitoInvito> selezionaInvitoUtenteTeam() {
        try {
            var utenti = control.richiediUtentiInvitabili(recuperaUtenteAutenticato());
            return utenti.isEmpty()
                    ? mostraNessunUtenteDisponibile() : mostraUtentiInvitabili(utenti);
        } catch (SessioneUtente.UtenteNonAutenticatoException errore) {
            return esito(401, "Accesso richiesto");
        } catch (RuntimeException errore) {
            return esito(500, "Recupero degli utenti non completato");
        }
    }

    @PostMapping
    public ResponseEntity<EsitoInvito> invia(@RequestBody RichiestaInvito richiesta) {
        try {
            Utente utente = recuperaUtenteAutenticato();
            if (richiesta == null || richiesta.utenteInvitatoId() == null
                    || richiesta.utenteInvitatoId() <= 0) {
                return mostraSelezioneUtenteObbligatoria();
            }
            control.richiediInvito(utente, new Utente(richiesta.utenteInvitatoId()));
            return mostraInvitoRegistrato();
        } catch (SessioneUtente.UtenteNonAutenticatoException errore) {
            return esito(401, "Accesso richiesto");
        } catch (TeamRepository.TeamNonCreatoException errore) {
            return esito(409, "È necessario aver creato un team");
        } catch (IllegalArgumentException errore) {
            return esito(400, "L'utente selezionato non è invitabile");
        } catch (RuntimeException errore) {
            return esito(500, "Invito non registrato");
        }
    }

    public ResponseEntity<EsitoInvito> selezionaUtente(Utente utenteInvitato) {
        return invia(new RichiestaInvito(utenteInvitato == null ? null : utenteInvitato.getId()));
    }

    public Utente recuperaUtenteAutenticato() {
        return sessioneUtente.recupera();
    }

    public ResponseEntity<EsitoInvito> mostraUtentiInvitabili(List<Utente> utentiInvitabili) {
        return ResponseEntity.ok(new EsitoInvito("Selezionare un utente", utentiInvitabili.stream()
                .map(utente -> new UtenteInvitabile(utente.getId(), utente.recuperaEmail()))
                .toList()));
    }

    public ResponseEntity<EsitoInvito> mostraNessunUtenteDisponibile() {
        return esito(200, "Nessun utente disponibile");
    }

    public ResponseEntity<EsitoInvito> mostraSelezioneUtenteObbligatoria() {
        return esito(400, "È necessario selezionare un utente");
    }

    public ResponseEntity<EsitoInvito> mostraInvitoRegistrato() {
        return esito(201, "Invito registrato");
    }

    private ResponseEntity<EsitoInvito> esito(int stato, String messaggio) {
        return ResponseEntity.status(stato).body(new EsitoInvito(messaggio, List.of()));
    }

    public record RichiestaInvito(Long utenteInvitatoId) { }
    public record UtenteInvitabile(Long id, String email) { }
    public record EsitoInvito(String messaggio, List<UtenteInvitabile> utentiInvitabili) { }
}
