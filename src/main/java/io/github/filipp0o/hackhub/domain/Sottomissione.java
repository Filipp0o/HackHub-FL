package io.github.filipp0o.hackhub.domain;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Sottomissione {

    private static final AtomicLong SEQUENZA_ID =
            new AtomicLong(1);

    private final Long id;
    private final String contenuto;

    private final Partecipazione partecipazione;
    private Valutazione valutazione;

    public Sottomissione(
            Partecipazione partecipazione,
            String contenuto
    ) {
        this.partecipazione = Objects.requireNonNull(
                partecipazione,
                "La partecipazione è obbligatoria"
        );

        if (contenuto == null || contenuto.isBlank()) {
            throw new IllegalArgumentException(
                    "Il contenuto della sottomissione è obbligatorio"
            );
        }

        this.contenuto = contenuto;

        partecipazione.registraSottomissione(this);

        this.id = SEQUENZA_ID.getAndIncrement();
    }

    public static Sottomissione crea(
            Partecipazione partecipazione,
            String contenuto
    ) {
        return new Sottomissione(
                partecipazione,
                contenuto
        );
    }

    void registraValutazione(
            Valutazione valutazione
    ) {
        Valutazione valutazioneValida =
                Objects.requireNonNull(
                        valutazione,
                        "La valutazione è obbligatoria"
                );

        if (this.valutazione != null) {
            throw new IllegalStateException(
                    "La sottomissione è già stata valutata"
            );
        }

        if (valutazioneValida.getSottomissione() != this) {
            throw new IllegalArgumentException(
                    "La valutazione deve riferirsi a questa sottomissione"
            );
        }

        this.valutazione = valutazioneValida;
    }

    public Long getId() {
        return id;
    }

    public String getContenuto() {
        return contenuto;
    }

    public Partecipazione getPartecipazione() {
        return partecipazione;
    }

    public Valutazione getValutazione() {
        return valutazione;
    }
}