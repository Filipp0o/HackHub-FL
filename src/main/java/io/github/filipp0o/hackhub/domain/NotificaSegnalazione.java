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
        this.segnalazione = Objects.requireNonNull(
                segnalazione,
                "La segnalazione è obbligatoria"
        );

        this.destinatario = Objects.requireNonNull(
                destinatario,
                "Il destinatario è obbligatorio"
        );

        this.dataOraCreazione = LocalDateTime.now();
        this.letta = false;

        this.segnalazione.registraNotificaSegnalazione(this);
    }

    public static NotificaSegnalazione crea(
            Segnalazione segnalazione,
            Utente destinatario
    ) {
        return new NotificaSegnalazione(
                segnalazione,
                destinatario
        );
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