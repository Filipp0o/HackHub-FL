package io.github.filipp0o.hackhub.infrastructure;

import io.github.filipp0o.hackhub.application.SegnalazioneRepository;
import io.github.filipp0o.hackhub.domain.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class SegnalazioneRepositoryImpl implements SegnalazioneRepository {

    private final Map<Long, Segnalazione> segnalazioni = new LinkedHashMap<>();
    private final Map<Long, NotificaSegnalazione> notifiche = new LinkedHashMap<>();

    private long prossimoIdSegnalazione = 1;
    private long prossimoIdNotifica = 1;

    @Override
    public synchronized List<Segnalazione> ottieniSegnalazioniDaEsaminare(
            Utente organizzatore
    ) {
        Objects.requireNonNull(organizzatore, "L'organizzatore è obbligatorio");

        return segnalazioni.values().stream()
                .filter(s -> s.getStato() == StatoSegnalazione.DA_ESAMINARE)
                .filter(s -> Objects.equals(
                        s.getPartecipazione().getHackathon().getOrganizzatore().getId(),
                        organizzatore.getId()
                ))
                .toList();
    }

    @Override
    public synchronized List<NotificaSegnalazione> ottieniNotificheRicevute(
            Utente destinatario
    ) {
        Objects.requireNonNull(destinatario, "Il destinatario è obbligatorio");

        return notifiche.values().stream()
                .filter(n -> Objects.equals(
                        n.getDestinatario().getId(),
                        destinatario.getId()
                ))
                .toList();
    }

    @Override
    public synchronized void salva(Segnalazione segnalazione) {
        Objects.requireNonNull(segnalazione, "La segnalazione è obbligatoria");
        verificaSegnalazione(segnalazione);
        registraSegnalazione(segnalazione);
    }

    @Override
    public synchronized void salvaConNotifica(
            Segnalazione segnalazione,
            NotificaSegnalazione notifica
    ) {
        Objects.requireNonNull(segnalazione, "La segnalazione è obbligatoria");
        Objects.requireNonNull(notifica, "La notifica è obbligatoria");

        if (notifica.getSegnalazione() != segnalazione) {
            throw new IllegalArgumentException(
                    "La notifica deve riferirsi alla segnalazione salvata"
            );
        }

        verificaSegnalazione(segnalazione);
        verificaNotifica(notifica);

        registraSegnalazione(segnalazione);
        registraNotifica(notifica);
    }

    @Override
    public synchronized void salvaNotifica(NotificaSegnalazione notifica) {
        Objects.requireNonNull(notifica, "La notifica è obbligatoria");

        if (notifica.getSegnalazione().getId() == null
                || !segnalazioni.containsKey(notifica.getSegnalazione().getId())) {
            throw new IllegalStateException(
                    "La segnalazione deve essere già salvata"
            );
        }

        verificaNotifica(notifica);
        registraNotifica(notifica);
    }

    private void verificaSegnalazione(Segnalazione s) {
        if (s.getId() != null && segnalazioni.get(s.getId()) != s) {
            throw new IllegalStateException(
                    "Segnalazione non appartenente al repository"
            );
        }
    }

    private void verificaNotifica(NotificaSegnalazione n) {
        if (n.getId() != null && notifiche.get(n.getId()) != n) {
            throw new IllegalStateException(
                    "Notifica non appartenente al repository"
            );
        }

        if (notifiche.values().stream().anyMatch(
                esistente -> esistente != n
                        && esistente.getSegnalazione() == n.getSegnalazione()
        )) {
            throw new IllegalStateException(
                    "La segnalazione possiede già una notifica salvata"
            );
        }
    }

    private void registraSegnalazione(Segnalazione s) {
        if (s.getId() == null) {
            s.assegnaId(prossimoIdSegnalazione++);
        }
        segnalazioni.put(s.getId(), s);
    }

    private void registraNotifica(NotificaSegnalazione n) {
        if (n.getId() == null) {
            n.assegnaId(prossimoIdNotifica++);
        }
        notifiche.put(n.getId(), n);
    }
}