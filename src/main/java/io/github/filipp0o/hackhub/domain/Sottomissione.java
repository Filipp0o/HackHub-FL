package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public class Sottomissione {

    private Long id;
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
    }

    void registraValutazione(Valutazione valutazione) {
        Valutazione valutazioneValida = Objects.requireNonNull(
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