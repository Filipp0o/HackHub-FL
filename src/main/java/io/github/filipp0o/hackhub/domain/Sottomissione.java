package io.github.filipp0o.hackhub.domain;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

public class Sottomissione {

    private static final AtomicLong SEQUENZA_ID =
            new AtomicLong(1);

    private final Long id;
    private String contenuto;

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

        this.contenuto = validaContenuto(contenuto);

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

    public String ottieniContenuto() {
        return contenuto;
    }

    public void aggiornaContenuto(
            String nuovoContenuto
    ) {
        contenuto = validaContenuto(
                nuovoContenuto
        );
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

    private static String validaContenuto(
            String contenuto
    ) {
        if (contenuto == null || contenuto.isBlank()) {
            throw new IllegalArgumentException(
                    "Il contenuto della sottomissione è obbligatorio"
            );
        }

        return contenuto;
    }
}