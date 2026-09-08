package io.github.filipp0o.hackhub.presentation;

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

    @ExceptionHandler(
            SessioneUtente.UtenteNonAutenticatoException.class
    )
    public ResponseEntity<ErroreRichiesta> accessoRichiesto() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErroreRichiesta(
                        "Accesso richiesto"
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErroreRichiesta> richiestaNonValida(
            IllegalArgumentException errore
    ) {
        return ResponseEntity.badRequest()
                .body(new ErroreRichiesta(
                        errore.getMessage()
                ));
    }

    public record ErroreRichiesta(
            String messaggio
    ) {
    }
}