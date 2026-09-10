package io.github.filipp0o.hackhub.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class NotificaSegnalazione {

    private Long id;
    private final LocalDateTime dataOraCreazione;
    private Boolean letta;

    private final Segnalazione segnalazione;
    private final Utente destinatario;

    private NotificaSegnalazione(
            Segnalazione segnalazione,
            Utente destinatario
    ) {
        this(null, segnalazione, destinatario, LocalDateTime.now(), false);
    }

    private NotificaSegnalazione(
            Long id,
            Segnalazione segnalazione,
            Utente destinatario,
            LocalDateTime dataOraCreazione,
            Boolean letta
    ) {
        this.segnalazione = Objects.requireNonNull(
                segnalazione, "La segnalazione è obbligatoria"
        );
        this.destinatario = Objects.requireNonNull(
                destinatario, "Il destinatario è obbligatorio"
        );
        this.dataOraCreazione = Objects.requireNonNull(
                dataOraCreazione, "La data di creazione è obbligatoria"
        );
        this.letta = Objects.requireNonNull(
                letta, "Lo stato di lettura è obbligatorio"
        );

        if (id != null) {
            assegnaId(id);
        }

        this.segnalazione.registraNotificaSegnalazione(this);
    }

    public static NotificaSegnalazione crea(
            Segnalazione segnalazione,
            Utente destinatario
    ) {
        return new NotificaSegnalazione(segnalazione, destinatario);
    }

    public static NotificaSegnalazione ricostruisci(
            Long id,
            Segnalazione segnalazione,
            Utente destinatario,
            LocalDateTime dataOraCreazione,
            Boolean letta
    ) {
        Objects.requireNonNull(id, "L'id della notifica è obbligatorio");
        return new NotificaSegnalazione(
                id, segnalazione, destinatario, dataOraCreazione, letta
        );
    }

    public void assegnaId(Long id) {
        Objects.requireNonNull(id, "L'id della notifica è obbligatorio");

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "L'id della notifica deve essere maggiore di zero"
            );
        }

        if (this.id != null) {
            throw new IllegalStateException(
                    "L'id della notifica è già stato assegnato"
            );
        }

        this.id = id;
    }

    public void segnaComeLetta() {
        this.letta = true;
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getDataOraCreazione() {
        return dataOraCreazione;
    }

    public Boolean getLetta() {
        return letta;
    }

    public Segnalazione getSegnalazione() {
        return segnalazione;
    }

    public Utente getDestinatario() {
        return destinatario;
    }
}