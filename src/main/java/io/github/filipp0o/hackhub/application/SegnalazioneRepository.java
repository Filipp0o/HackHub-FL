package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.Utente;

import java.util.List;

public interface SegnalazioneRepository {

    List<Segnalazione> ottieniSegnalazioniDaEsaminare(
            Utente organizzatore
    );

    void salva(Segnalazione segnalazione);

    void salvaConNotifica(
            Segnalazione segnalazione,
            NotificaSegnalazione notifica
    );

    void salvaNotifica(NotificaSegnalazione notifica);
}