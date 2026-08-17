package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.SegnalazioneRepository;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.StatoSegnalazione;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class SegnalazioneRepositoryImpl
        implements SegnalazioneRepository {

    private final List<Segnalazione> segnalazioniSalvate =
            new ArrayList<>();

    private final List<NotificaSegnalazione> notificheSalvate =
            new ArrayList<>();

    @Override
    public List<Segnalazione> ottieniSegnalazioniDaEsaminare(
            Utente organizzatore
    ) {
        Utente organizzatoreValido =
                Objects.requireNonNull(
                        organizzatore,
                        "L'organizzatore è obbligatorio"
                );

        return segnalazioniSalvate.stream()
                .filter(segnalazione ->
                        segnalazione.getStato()
                                == StatoSegnalazione.DA_ESAMINARE
                )
                .filter(segnalazione ->
                        Objects.equals(
                                segnalazione
                                        .getPartecipazione()
                                        .getHackathon()
                                        .getOrganizzatore()
                                        .getId(),
                                organizzatoreValido.getId()
                        )
                )
                .toList();
    }

    @Override
    public void salva(Segnalazione segnalazione) {
        segnalazioniSalvate.add(
                Objects.requireNonNull(
                        segnalazione,
                        "La segnalazione è obbligatoria"
                )
        );
    }

    @Override
    public void salvaConNotifica(
            Segnalazione segnalazione,
            NotificaSegnalazione notifica
    ) {
        Segnalazione segnalazioneValida =
                Objects.requireNonNull(
                        segnalazione,
                        "La segnalazione è obbligatoria"
                );

        NotificaSegnalazione notificaValida =
                Objects.requireNonNull(
                        notifica,
                        "La notifica è obbligatoria"
                );

        if (notificaValida.getSegnalazione()
                != segnalazioneValida) {
            throw new IllegalArgumentException(
                    "La notifica deve riferirsi alla segnalazione salvata"
            );
        }

        segnalazioniSalvate.add(segnalazioneValida);
        notificheSalvate.add(notificaValida);
    }

    @Override
    public void salvaNotifica(
            NotificaSegnalazione notifica
    ) {
        notificheSalvate.add(
                Objects.requireNonNull(
                        notifica,
                        "La notifica è obbligatoria"
                )
        );
    }
}