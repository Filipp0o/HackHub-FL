package io.github.filipp0o.hackhub.domain;

import java.time.LocalDateTime;
import java.util.Objects;

public class Segnalazione {

    private Long id;
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

        this.descrizione = descrizione;
        this.dataOraCreazione = LocalDateTime.now();
        this.stato = StatoSegnalazione.DA_ESAMINARE;
    }

    private Segnalazione(DatiRipristinoSegnalazione dati) {
        Objects.requireNonNull(
                dati,
                "I dati di ripristino sono obbligatori"
        );

        mentoreSegnalante = Objects.requireNonNull(
                dati.mentoreSegnalante(),
                "Il mentore segnalante è obbligatorio"
        );

        partecipazione = Objects.requireNonNull(
                dati.partecipazione(),
                "La partecipazione è obbligatoria"
        );

        if (dati.descrizione() == null || dati.descrizione().isBlank()) {
            throw new IllegalArgumentException(
                    "La descrizione della violazione è obbligatoria"
            );
        }

        descrizione = dati.descrizione();

        dataOraCreazione = Objects.requireNonNull(
                dati.dataOraCreazione(),
                "La data di creazione è obbligatoria"
        );

        stato = Objects.requireNonNull(
                dati.stato(),
                "Lo stato è obbligatorio"
        );

        if (stato == StatoSegnalazione.DA_ESAMINARE) {
            if (dati.esito() != null
                    || dati.motivazione() != null
                    || dati.dataOraEsame() != null
                    || dati.esaminatore() != null) {
                throw new IllegalArgumentException(
                        "Una segnalazione da esaminare non ha una decisione"
                );
            }
        } else if (dati.esito() == null
                || dati.motivazione() == null
                || dati.motivazione().isBlank()
                || dati.dataOraEsame() == null
                || dati.esaminatore() == null) {
            throw new IllegalArgumentException(
                    "La decisione della segnalazione esaminata è incompleta"
            );
        }

        esito = dati.esito();
        motivazione = dati.motivazione();
        dataOraEsame = dati.dataOraEsame();
        esaminatore = dati.esaminatore();

        assegnaId(dati.id());
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

    public static Segnalazione ricostruisci(
            DatiRipristinoSegnalazione dati
    ) {
        return new Segnalazione(dati);
    }

    public void assegnaId(Long id) {
        Objects.requireNonNull(
                id,
                "L'id della segnalazione è obbligatorio"
        );

        if (id <= 0) {
            throw new IllegalArgumentException(
                    "L'id della segnalazione deve essere maggiore di zero"
            );
        }

        if (this.id != null) {
            throw new IllegalStateException(
                    "L'id della segnalazione è già stato assegnato"
            );
        }

        this.id = id;
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

    public void annullaEsameNonRegistrato() {
        stato = StatoSegnalazione.DA_ESAMINARE;
        esito = null;
        motivazione = null;
        esaminatore = null;
        dataOraEsame = null;
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