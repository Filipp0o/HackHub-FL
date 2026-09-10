package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiDecisioneSegnalazione;
import io.github.filipp0o.hackhub.domain.EsitoSegnalazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.StatoPartecipazione;
import io.github.filipp0o.hackhub.domain.StatoSegnalazione;
import io.github.filipp0o.hackhub.domain.Utente;
import org.springframework.transaction.support.TransactionOperations;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Objects;

public class EsaminareSegnalazioneControl {

    private final SegnalazioneRepository segnalazioneRepository;
    private final PartecipazioneRepository partecipazioneRepository;
    private final TransactionOperations transazioni;

    public EsaminareSegnalazioneControl(
            SegnalazioneRepository segnalazioneRepository,
            PartecipazioneRepository partecipazioneRepository
    ) {
        this(
                segnalazioneRepository,
                partecipazioneRepository,
                TransactionOperations.withoutTransaction()
        );
    }

    public EsaminareSegnalazioneControl(
            SegnalazioneRepository segnalazioneRepository,
            PartecipazioneRepository partecipazioneRepository,
            TransactionOperations transazioni
    ) {
        this.transazioni = Objects.requireNonNull(
                transazioni,
                "La gestione delle transazioni è obbligatoria"
        );

        this.segnalazioneRepository = Objects.requireNonNull(
                segnalazioneRepository,
                "Il repository delle segnalazioni è obbligatorio"
        );

        this.partecipazioneRepository = Objects.requireNonNull(
                partecipazioneRepository,
                "Il repository delle partecipazioni è obbligatorio"
        );
    }

    public List<Segnalazione> avviaEsameSegnalazioni(
            Utente organizzatore
    ) {
        Utente organizzatoreValido = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );

        return segnalazioneRepository.ottieniSegnalazioniDaEsaminare(
                organizzatoreValido
        );
    }

    public Segnalazione selezionaSegnalazione(
            Segnalazione segnalazione,
            Utente organizzatore
    ) {
        Segnalazione segnalazioneValida = Objects.requireNonNull(
                segnalazione,
                "La segnalazione è obbligatoria"
        );

        Utente organizzatoreValido = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );

        Hackathon hackathon = segnalazioneValida
                .getPartecipazione()
                .getHackathon();

        if (!Objects.equals(
                hackathon.getOrganizzatore().getId(),
                organizzatoreValido.getId()
        )) {
            throw new IllegalArgumentException(
                    "L'organizzatore non è autorizzato a esaminare questa segnalazione"
            );
        }

        if (segnalazioneValida.getStato()
                != StatoSegnalazione.DA_ESAMINARE) {
            throw new IllegalStateException(
                    "La segnalazione è già stata esaminata"
            );
        }

        return segnalazioneValida;
    }

    public Segnalazione apriSegnalazioneDaNotifica(
            NotificaSegnalazione notificaSegnalazione,
            Utente organizzatore
    ) {
        NotificaSegnalazione notificaValida = Objects.requireNonNull(
                notificaSegnalazione,
                "La notifica della segnalazione è obbligatoria"
        );

        Utente organizzatoreValido = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );

        if (!Objects.equals(
                notificaValida.getDestinatario().getId(),
                organizzatoreValido.getId()
        )) {
            throw new IllegalArgumentException(
                    "La notifica non è destinata a questo organizzatore"
            );
        }

        Segnalazione segnalazione = notificaValida.getSegnalazione();

        Hackathon hackathon = segnalazione
                .getPartecipazione()
                .getHackathon();

        if (!Objects.equals(
                hackathon.getOrganizzatore().getId(),
                organizzatoreValido.getId()
        )) {
            throw new IllegalArgumentException(
                    "L'organizzatore non è autorizzato a esaminare questa segnalazione"
            );
        }

        if (segnalazione.getStato() != StatoSegnalazione.DA_ESAMINARE) {
            throw new IllegalStateException(
                    "La segnalazione è già stata esaminata"
            );
        }

        Boolean lettaPrima = notificaValida.getLetta();

        try {
            if (TransactionSynchronizationManager.isSynchronizationActive()) {
                boolean giaRegistrato =
                        TransactionSynchronizationManager.getSynchronizations()
                                .stream()
                                .anyMatch(s -> s instanceof RipristinoLettura r
                                        && r.notifica() == notificaValida);

                if (!giaRegistrato) {
                    TransactionSynchronizationManager.registerSynchronization(
                            new RipristinoLettura(notificaValida, lettaPrima)
                    );
                }
            }

            notificaValida.segnaComeLetta();
            segnalazioneRepository.salvaNotifica(notificaValida);
        } catch (RuntimeException errore) {
            notificaValida.ripristinaLettura(lettaPrima);

            throw new IllegalStateException(
                    "La lettura della notifica non è stata registrata",
                    errore
            );
        }

        return segnalazione;
    }

    private record RipristinoLettura(
            NotificaSegnalazione notifica,
            Boolean lettaPrima
    ) implements TransactionSynchronization {

        @Override
        public void afterCompletion(int stato) {
            if (stato == STATUS_ROLLED_BACK) {
                notifica.ripristinaLettura(lettaPrima);
            }
        }
    }

    public void verificaDecisione(
            DatiDecisioneSegnalazione dati
    ) {
        validaCompletezzaDecisione(dati);
    }

    public void registraDecisione(
            Segnalazione segnalazione,
            Utente organizzatore,
            DatiDecisioneSegnalazione dati
    ) {
        Segnalazione segnalazioneValida = Objects.requireNonNull(
                segnalazione,
                "La segnalazione è obbligatoria"
        );

        Utente organizzatoreValido = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );

        validaCompletezzaDecisione(dati);

        Partecipazione partecipazione =
                segnalazioneValida.getPartecipazione();

        Hackathon hackathon = partecipazione.getHackathon();

        if (!Objects.equals(
                hackathon.getOrganizzatore().getId(),
                organizzatoreValido.getId()
        )) {
            throw new IllegalArgumentException(
                    "L'organizzatore non è autorizzato a esaminare questa segnalazione"
            );
        }

        if (segnalazioneValida.getStato()
                != StatoSegnalazione.DA_ESAMINARE) {
            throw new IllegalStateException(
                    "La segnalazione è già stata esaminata"
            );
        }

        StatoPartecipazione statoPrecedente = partecipazione.getStato();

        Runnable ripristino = () -> {
            segnalazioneValida.annullaEsameNonRegistrato();
            partecipazione.ripristinaStato(statoPrecedente);
        };

        try {
            transazioni.executeWithoutResult(stato -> {
                if (TransactionSynchronizationManager.isSynchronizationActive()) {
                    TransactionSynchronizationManager.registerSynchronization(
                            new TransactionSynchronization() {
                                @Override
                                public void afterCompletion(int esitoTransazione) {
                                    if (esitoTransazione == STATUS_ROLLED_BACK) {
                                        ripristino.run();
                                    }
                                }
                            }
                    );
                }

                segnalazioneValida.registraEsame(
                        dati,
                        organizzatoreValido
                );

                if (dati.esito()
                        == EsitoSegnalazione.VIOLAZIONE_CON_ESCLUSIONE) {
                    partecipazione.escludi();
                    partecipazioneRepository.salva(partecipazione);
                }

                segnalazioneRepository.salva(segnalazioneValida);
            });
        } catch (RuntimeException errore) {
            ripristino.run();
            throw new RegistrazioneDecisioneFallitaException(errore);
        }
    }

    public static class RegistrazioneDecisioneFallitaException
            extends IllegalStateException {

        public RegistrazioneDecisioneFallitaException(Throwable causa) {
            super("La decisione non è stata registrata", causa);
        }
    }

    private void validaCompletezzaDecisione(
            DatiDecisioneSegnalazione dati
    ) {
        DatiDecisioneSegnalazione datiValidi = Objects.requireNonNull(
                dati,
                "I dati della decisione sono obbligatori"
        );

        Objects.requireNonNull(
                datiValidi.esito(),
                "L'esito della segnalazione è obbligatorio"
        );

        if (datiValidi.motivazione() == null
                || datiValidi.motivazione().isBlank()) {
            throw new IllegalArgumentException(
                    "La motivazione della decisione è obbligatoria"
            );
        }
    }
}