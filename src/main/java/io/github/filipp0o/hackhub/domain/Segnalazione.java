package io.github.filipp0o.hackhub.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Segnalazione {

    private static final AtomicLong SEQUENZA_ID =
            new AtomicLong(1);

    private final Long id;
    private final String descrizione;
    private final LocalDateTime dataOraCreazione;

    private StatoSegnalazione stato;
    private EsitoSegnalazione esito;
    private String motivazione;
    private LocalDateTime dataOraEsame;
    private final Utente mentoreSegnalante;
    private final Partecipazione partecipazione;
    private Utente esaminatore;
    private NotificaSegnalazione notificaSegnalazione;

    private Segnalazione(
            Utente mentoreSegnalante,
            Partecipazione partecipazione,
            String descrizione
    ) {
        this.mentoreSegnalante = Objects.requireNonNull(
                mentoreSegnalante,
                "Il mentore segnalante è obbligatorio"
        );

        this.partecipazione = Objects.requireNonNull(
                partecipazione,
                "La partecipazione è obbligatoria"
        );

        Hackathon hackathon = partecipazione.getHackathon();

        if (!hackathon.consenteSegnalazioni()) {
            throw new IllegalStateException(
                    "La segnalazione può essere creata solo per un hackathon in corso o in valutazione"
            );
        }

        boolean mentoreAssegnato = hackathon
                .getMentori()
                .stream()
                .anyMatch(mentore ->
                        Objects.equals(
                                mentore.getId(),
                                mentoreSegnalante.getId()
                        )
                );

        if (!mentoreAssegnato) {
            throw new IllegalArgumentException(
                    "Il mentore segnalante deve essere assegnato all'hackathon"
            );
        }

        if (descrizione == null || descrizione.isBlank()) {
            throw new IllegalArgumentException(
                    "La descrizione della violazione è obbligatoria"
            );
        }

        this.id = SEQUENZA_ID.getAndIncrement();
        this.descrizione = descrizione;
        this.dataOraCreazione = LocalDateTime.now();
        this.stato = StatoSegnalazione.DA_ESAMINARE;
    }

    public static Segnalazione crea(
            Utente mentoreSegnalante,
            Partecipazione partecipazione,
            String descrizione
    ) {
        return new Segnalazione(
                mentoreSegnalante,
                partecipazione,
                descrizione
        );
    }

    void registraNotificaSegnalazione(
            NotificaSegnalazione notificaSegnalazione
    ) {
        NotificaSegnalazione notificaValida =
                Objects.requireNonNull(
                        notificaSegnalazione,
                        "La notifica della segnalazione è obbligatoria"
                );

        if (this.notificaSegnalazione != null) {
            throw new IllegalStateException(
                    "La segnalazione possiede già una notifica"
            );
        }

        if (notificaValida.getSegnalazione() != this) {
            throw new IllegalArgumentException(
                    "La notifica deve riferirsi a questa segnalazione"
            );
        }

        this.notificaSegnalazione = notificaValida;
    }

    public void registraEsame(
            DatiDecisioneSegnalazione dati,
            Utente esaminatore
    ) {
        if (stato != StatoSegnalazione.DA_ESAMINARE) {
            throw new IllegalStateException(
                    "La segnalazione è già stata esaminata"
            );
        }

        Objects.requireNonNull(
                dati,
                "I dati della decisione sono obbligatori"
        );

        EsitoSegnalazione esito = Objects.requireNonNull(
                dati.esito(),
                "L'esito della segnalazione è obbligatorio"
        );

        if (dati.motivazione() == null
                || dati.motivazione().isBlank()) {
            throw new IllegalArgumentException(
                    "La motivazione della decisione è obbligatoria"
            );
        }

        Utente esaminatoreValido = Objects.requireNonNull(
                esaminatore,
                "L'esaminatore è obbligatorio"
        );

        this.esito = esito;
        this.motivazione = dati.motivazione();
        this.esaminatore = esaminatoreValido;
        this.dataOraEsame = LocalDateTime.now();
        this.stato = StatoSegnalazione.ESAMINATA;
    }

    public Long getId() {
        return id;
    }

    public String getDescrizione() {
        return descrizione;
    }

    public LocalDateTime getDataOraCreazione() {
        return dataOraCreazione;
    }

    public StatoSegnalazione getStato() {
        return stato;
    }

    public EsitoSegnalazione getEsito() {
        return esito;
    }

    public String getMotivazione() {
        return motivazione;
    }

    public LocalDateTime getDataOraEsame() {
        return dataOraEsame;
    }

    public Utente getMentoreSegnalante() {
        return mentoreSegnalante;
    }

    public Partecipazione getPartecipazione() {
        return partecipazione;
    }

    public Utente getEsaminatore() {
        return esaminatore;
    }

    public NotificaSegnalazione getNotificaSegnalazione() {
        return notificaSegnalazione;
    }
}