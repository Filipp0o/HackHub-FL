package io.github.filipp0o.hackhub.presentation;

import io.github.filipp0o.hackhub.application.*;
import io.github.filipp0o.hackhub.domain.Hackathon;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;

final class SupportoRest {

    private SupportoRest() {
    }

    static void id(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException(
                    "L'identificativo deve essere positivo"
            );
        }
    }

    static void richiesta(Object richiesta) {
        if (richiesta == null) {
            throw new IllegalArgumentException(
                    "La richiesta è obbligatoria"
            );
        }
    }

    static void autorizza(Long autorizzato, Long corrente) {
        if (corrente == null || !Objects.equals(autorizzato, corrente)) {
            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Operazione non autorizzata"
            );
        }
    }

    static Hackathon hackathon(
            HackathonRepository repository,
            Long id
    ) {
        id(id);

        try {
            Hackathon hackathon = repository.recuperaHackathon(id);

            if (hackathon == null) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Hackathon non trovato"
                );
            }

            return hackathon;
        } catch (IllegalStateException errore) {
            if (errore.getCause() == null
                    && "Hackathon non trovato".equals(errore.getMessage())) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        errore.getMessage(),
                        errore
                );
            }

            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Recupero dell'hackathon fallito",
                    errore
            );
        }
    }

    static <T> T leggi(Supplier<T> lettura) {
        try {
            return lettura.get();
        } catch (ResponseStatusException errore) {
            throw errore;
        } catch (RuntimeException errore) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Lettura dei dati non completata",
                    errore
            );
        }
    }

    static <T> T esegui(Supplier<T> operazione) {
        try {
            return operazione.get();
        } catch (SistemaPagamentoGateway.ConfigurazioneAnnullataException errore) {
            throw errore;
        } catch (
                InviareSottomissioneControl.RegistrazioneSottomissioneFallitaException
                | AggiornareSottomissioneControl.AggiornamentoSottomissioneFallitoException
                | ProclamareTeamVincitoreControl.RegistrazioneProclamazioneFallitaException
                | ErogarePremioControl.ErogazioneFallitaException
                | ConfigurareRiscossionePremioControl.ConfigurazioneFallitaException
                | EsaminareSegnalazioneControl.RegistrazioneDecisioneFallitaException
                | EsaminareSegnalazioneControl.RegistrazioneLetturaFallitaException errore
        ) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    errore.getMessage(),
                    errore
            );
        } catch (NoSuchElementException errore) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    errore.getMessage(),
                    errore
            );
        } catch (IllegalStateException errore) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    errore.getMessage(),
                    errore
            );
        } catch (NullPointerException errore) {
            Set<String> risorseMancanti = Set.of(
                    "Il team dell'utente è obbligatorio",
                    "La partecipazione è obbligatoria",
                    "La sottomissione è obbligatoria"
            );

            if (risorseMancanti.contains(
                    Objects.toString(errore.getMessage(), "")
            )) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        errore.getMessage(),
                        errore
                );
            }

            throw errore;
        }
    }
}