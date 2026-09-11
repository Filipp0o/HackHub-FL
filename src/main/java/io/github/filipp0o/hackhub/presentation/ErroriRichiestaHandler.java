package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.CreareHackathonControl;
import io.github.filipp0o.hackhub.application.CreareTeamControl;
import io.github.filipp0o.hackhub.application.EsaminareSegnalazioneControl;
import io.github.filipp0o.hackhub.application.ValutareSottomissioneControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {
        CreareTeamBoundary.class,
        CreareHackathonBoundary.class,
        IscrivereTeamHackathonBoundary.class,
        SegnalareViolazioneBoundary.class,
        ValutareSottomissioneBoundary.class,
        EsaminareSegnalazioneBoundary.class,
        InviareSottomissioneBoundary.class,
        AggiornareSottomissioneBoundary.class,
        ProclamareTeamVincitoreBoundary.class,
        ErogarePremioBoundary.class,
        ConfigurareRiscossionePremioBoundary.class
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

    @ExceptionHandler(
            EsaminareSegnalazioneControl.RegistrazioneDecisioneFallitaException.class
    )
    public ResponseEntity<ErroreRichiesta> registrazioneDecisioneFallita(
            EsaminareSegnalazioneControl.RegistrazioneDecisioneFallitaException errore
    ) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroreRichiesta(errore.getMessage()));
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErroreRichiesta> erroreHttp(
            ResponseStatusException errore
    ) {
        return ResponseEntity.status(errore.getStatusCode())
                .body(new ErroreRichiesta(errore.getReason()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroreRichiesta> erroreImprevisto(
            Exception errore
    ) {
        if (errore instanceof HttpMessageNotReadableException
                || errore instanceof ServletRequestBindingException
                || errore instanceof MethodArgumentTypeMismatchException) {
            return ResponseEntity.badRequest()
                    .body(new ErroreRichiesta(
                            "Richiesta mancante o non leggibile"
                    ));
        }

        if (errore instanceof ErrorResponse risposta) {
            return ResponseEntity.status(risposta.getStatusCode())
                    .body(new ErroreRichiesta(
                            "Richiesta non supportata"
                    ));
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErroreRichiesta("Operazione non completata"));
    }

    public record ErroreRichiesta(String messaggio) {
    }
}