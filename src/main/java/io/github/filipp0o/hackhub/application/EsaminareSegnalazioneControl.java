package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiDecisioneSegnalazione;
import io.github.filipp0o.hackhub.domain.EsitoSegnalazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.StatoSegnalazione;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;
import java.util.Objects;

public class EsaminareSegnalazioneControl {

    private final SegnalazioneRepository segnalazioneRepository;
    private final PartecipazioneRepository partecipazioneRepository;

    public EsaminareSegnalazioneControl(
            SegnalazioneRepository segnalazioneRepository,
            PartecipazioneRepository partecipazioneRepository
    ) {
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

        return segnalazioneRepository
                .ottieniSegnalazioniDaEsaminare(
                        organizzatoreValido
                );
    }

    public Segnalazione selezionaSegnalazione(
            Segnalazione segnalazione,
            Utente organizzatore
    ) {
        Segnalazione segnalazioneValida =
                Objects.requireNonNull(
                        segnalazione,
                        "La segnalazione è obbligatoria"
                );

        Utente organizzatoreValido =
                Objects.requireNonNull(
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
        NotificaSegnalazione notificaValida =
                Objects.requireNonNull(
                        notificaSegnalazione,
                        "La notifica della segnalazione è obbligatoria"
                );

        Utente organizzatoreValido =
                Objects.requireNonNull(
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

        Segnalazione segnalazione =
                notificaValida.getSegnalazione();

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

        if (segnalazione.getStato()
                != StatoSegnalazione.DA_ESAMINARE) {
            throw new IllegalStateException(
                    "La segnalazione è già stata esaminata"
            );
        }

        notificaValida.segnaComeLetta();

        segnalazioneRepository.salvaNotifica(
                notificaValida
        );

        return segnalazione;
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
        Segnalazione segnalazioneValida =
                Objects.requireNonNull(
                        segnalazione,
                        "La segnalazione è obbligatoria"
                );

        Utente organizzatoreValido =
                Objects.requireNonNull(
                        organizzatore,
                        "L'organizzatore è obbligatorio"
                );

        validaCompletezzaDecisione(dati);

        Partecipazione partecipazione =
                segnalazioneValida.getPartecipazione();

        Hackathon hackathon =
                partecipazione.getHackathon();

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

        segnalazioneValida.registraEsame(
                dati,
                organizzatoreValido
        );

        if (dati.esito()
                == EsitoSegnalazione.VIOLAZIONE_CON_ESCLUSIONE) {
            partecipazione.escludi();

            partecipazioneRepository.salva(
                    partecipazione
            );
        }

        segnalazioneRepository.salva(
                segnalazioneValida
        );
    }

    private void validaCompletezzaDecisione(
            DatiDecisioneSegnalazione dati
    ) {
        DatiDecisioneSegnalazione datiValidi =
                Objects.requireNonNull(
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