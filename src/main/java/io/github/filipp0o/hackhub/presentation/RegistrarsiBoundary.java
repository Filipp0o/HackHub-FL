package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.RegistrarsiControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.Objects;

@RestController
@RequestMapping("/api/registrazione")
public class RegistrarsiBoundary {

    private final RegistrarsiControl control;

    public RegistrarsiBoundary(RegistrarsiControl control) {
        this.control = Objects.requireNonNull(
                control,
                "Il control è obbligatorio"
        );
    }

    @GetMapping
    public ResponseEntity<EsitoRegistrazione> selezionaRegistrazione() {
        return mostraRichiestaDatiRegistrazione();
    }

    @PostMapping
    public ResponseEntity<EsitoRegistrazione> registra(
            @RequestBody RichiestaRegistrazione richiesta
    ) {
        if (richiesta == null) {
            return mostraDatiDaCorreggere();
        }

        return inserisciDatiRegistrazione(
                richiesta.email(),
                richiesta.password()
        );
    }

    public ResponseEntity<EsitoRegistrazione> inserisciDatiRegistrazione(
            String email,
            String password
    ) {
        try {
            control.richiediRegistrazione(email, password);
            return mostraRegistrazioneCompletata();
        } catch (RegistrarsiControl.EmailGiaRegistrataException errore) {
            return mostraEmailGiaRegistrata();
        } catch (IllegalArgumentException errore) {
            return ResponseEntity.badRequest().body(
                    new EsitoRegistrazione(errore.getMessage())
            );
        } catch (RuntimeException errore) {
            return mostraRegistrazioneNonCompletata();
        }
    }

    public ResponseEntity<EsitoRegistrazione>
    mostraRichiestaDatiRegistrazione() {
        return ResponseEntity.ok(
                new EsitoRegistrazione("Inserire email e password")
        );
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<EsitoRegistrazione> mostraDatiDaCorreggere() {
        return ResponseEntity.badRequest().body(
                new EsitoRegistrazione(
                        "Fornire email e password in una richiesta JSON valida"
                )
        );
    }

    public ResponseEntity<EsitoRegistrazione> mostraEmailGiaRegistrata() {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new EsitoRegistrazione("L'email è già registrata")
        );
    }

    public ResponseEntity<EsitoRegistrazione>
    mostraRegistrazioneNonCompletata() {
        return ResponseEntity.status(
                HttpStatus.INTERNAL_SERVER_ERROR
        ).body(
                new EsitoRegistrazione("Registrazione non completata")
        );
    }

    public ResponseEntity<EsitoRegistrazione>
    mostraRegistrazioneCompletata() {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new EsitoRegistrazione("Registrazione completata")
        );
    }

    public record RichiestaRegistrazione(
            String email,
            String password
    ) {
    }

    public record EsitoRegistrazione(String messaggio) {
    }
}