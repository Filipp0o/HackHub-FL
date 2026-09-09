package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareHackathonControl;
import io.github.filipp0o.hackhub.application.CreareTeamControl;
import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(assignableTypes = {
        CreareTeamBoundary.class,
        CreareHackathonBoundary.class,
        IscrivereTeamHackathonBoundary.class,
        SegnalareViolazioneBoundary.class,
        ValutareSottomissioneBoundary.class,
        EsaminareSegnalazioneBoundary.class
})
public class ErroriRichiestaHandler {

    @ExceptionHandler(SessioneUtente.UtenteNonAutenticatoException.class)
    public ResponseEntity<ErroreRichiesta> accessoRichiesto() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroreRichiesta("Accesso richiesto"));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroreRichiesta> richiestaNonValida(
            IllegalArgumentException errore
    ) {
        return ResponseEntity.badRequest()
                .body(new ErroreRichiesta(errore.getMessage()));
    }

    @ExceptionHandler(CreareTeamControl.UtenteGiaInTeamException.class)
    public ResponseEntity<ErroreRichiesta> utenteGiaInTeam(
            CreareTeamControl.UtenteGiaInTeamException errore
    ) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErroreRichiesta(errore.getMessage()));
    }

    @ExceptionHandler(CreareTeamControl.CreazioneTeamFallitaException.class)
    public ResponseEntity<ErroreRichiesta> creazioneTeamFallita(
            CreareTeamControl.CreazioneTeamFallitaException errore
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroreRichiesta(errore.getMessage()));
    }

    @ExceptionHandler(
            ValutareSottomissioneControl.RegistrazioneValutazioneFallitaException.class
    )
    public ResponseEntity<ErroreRichiesta> registrazioneValutazioneFallita(
            ValutareSottomissioneControl.RegistrazioneValutazioneFallitaException errore
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroreRichiesta(errore.getMessage()));
    }

    @ExceptionHandler(
            CreareHackathonControl.CreazioneHackathonFallitaException.class
    )
    public ResponseEntity<ErroreRichiesta> creazioneHackathonFallita(
            CreareHackathonControl.CreazioneHackathonFallitaException errore
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroreRichiesta(errore.getMessage()));
    }

    public record ErroreRichiesta(
            String messaggio
    ) {
    }
}