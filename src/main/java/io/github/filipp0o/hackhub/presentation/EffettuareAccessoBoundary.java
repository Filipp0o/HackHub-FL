package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.EffettuareAccessoControl;
import io.github.filipp0o.hackhub.domain.Utente;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/accesso")
public class EffettuareAccessoBoundary {

    private final EffettuareAccessoControl control;
    private final SessioneUtente sessioneUtente;
    private final HttpServletRequest richiestaHttp;

    public EffettuareAccessoBoundary(
            EffettuareAccessoControl control,
            SessioneUtente sessioneUtente,
            HttpServletRequest richiestaHttp
    ) {
        this.control = Objects.requireNonNull(control, "Il control è obbligatorio");
        this.sessioneUtente = Objects.requireNonNull(
                sessioneUtente, "La sessione utente è obbligatoria"
        );
        this.richiestaHttp = Objects.requireNonNull(
                richiestaHttp, "La richiesta HTTP è obbligatoria"
        );
    }

    @GetMapping
    public ResponseEntity<EsitoAccesso> selezionaAccesso() {
        return mostraRichiestaCredenziali();
    }

    @PostMapping
    public ResponseEntity<EsitoAccesso> accedi(
            @RequestBody RichiestaAccesso richiesta
    ) {
        if (richiesta == null) {
            return mostraCredenzialiNonValide();
        }
        return inserisciCredenziali(richiesta.email(), richiesta.password());
    }

    public ResponseEntity<EsitoAccesso> inserisciCredenziali(
            String email, String password
    ) {
        try {
            Utente utente = control.richiediAccesso(email, password);
            registraUtenteAutenticato(utente);
            return mostraAccessoEffettuato();
        } catch (EffettuareAccessoControl.CredenzialiNonValideException errore) {
            return mostraCredenzialiNonValide();
        } catch (RuntimeException errore) {
            return mostraAccessoNonCompletato();
        }
    }

    public void registraUtenteAutenticato(Utente utente) {
        Objects.requireNonNull(utente, "L'utente autenticato è obbligatorio");
        // La rotazione richiede una sessione esistente e ne conserva gli attributi.
        richiestaHttp.getSession();
        richiestaHttp.changeSessionId();
        sessioneUtente.registra(utente);
    }

    public ResponseEntity<EsitoAccesso> mostraRichiestaCredenziali() {
        return ResponseEntity.ok(new EsitoAccesso("Inserire email e password"));
    }

    public ResponseEntity<EsitoAccesso> mostraCredenzialiNonValide() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new EsitoAccesso("Credenziali non valide"));
    }

    public ResponseEntity<EsitoAccesso> mostraAccessoEffettuato() {
        return ResponseEntity.ok(new EsitoAccesso("Accesso effettuato"));
    }

    public ResponseEntity<EsitoAccesso> mostraAccessoNonCompletato() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new EsitoAccesso("Accesso non completato"));
    }

    public record RichiestaAccesso(String email, String password) {
    }

    public record EsitoAccesso(String messaggio) {
    }
}
