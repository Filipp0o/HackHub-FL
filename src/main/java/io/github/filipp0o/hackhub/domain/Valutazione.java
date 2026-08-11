package io.github.filipp0o.hackhub.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

public class Valutazione {

    private Long id;
    private final String giudizio;
    private final BigDecimal punteggio;
    private final LocalDateTime dataOra;

    private final Sottomissione sottomissione;
    private final Utente giudice;

    private Valutazione(
            Sottomissione sottomissione,
            Utente giudice,
            DatiValutazione dati
    ) {
        this.sottomissione = Objects.requireNonNull(
                sottomissione,
                "La sottomissione è obbligatoria"
        );

        this.giudice = Objects.requireNonNull(
                giudice,
                "Il giudice è obbligatorio"
        );

        Objects.requireNonNull(
                dati,
                "I dati della valutazione sono obbligatori"
        );

        if (dati.giudizio() == null
                || dati.giudizio().isBlank()) {
            throw new IllegalArgumentException(
                    "Il giudizio è obbligatorio"
            );
        }

        BigDecimal punteggio = Objects.requireNonNull(
                dati.punteggio(),
                "Il punteggio è obbligatorio"
        );

        if (punteggio.compareTo(BigDecimal.ZERO) < 0
                || punteggio.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException(
                    "Il punteggio deve essere compreso tra 0 e 10"
            );
        }

        this.giudizio = dati.giudizio();
        this.punteggio = punteggio;
        this.dataOra = LocalDateTime.now();
    }

    public static Valutazione crea(
            Sottomissione sottomissione,
            Utente giudice,
            DatiValutazione dati
    ) {
        Valutazione valutazione = new Valutazione(
                sottomissione,
                giudice,
                dati
        );

        sottomissione.registraValutazione(valutazione);

        return valutazione;
    }

    public Long getId() {
        return id;
    }

    public String getGiudizio() {
        return giudizio;
    }

    public BigDecimal getPunteggio() {
        return punteggio;
    }

    public LocalDateTime getDataOra() {
        return dataOra;
    }

    public Sottomissione getSottomissione() {
        return sottomissione;
    }

    public Utente getGiudice() {
        return giudice;
    }
}