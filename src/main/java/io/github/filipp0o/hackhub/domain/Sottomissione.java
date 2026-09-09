package io.github.filipp0o.hackhub.domain;

import java.util.Objects;

public class Sottomissione {

    private Long id;
    private String contenuto;

    private final Partecipazione partecipazione;
    private Valutazione valutazione;

    public Sottomissione(
            Partecipazione partecipazione,
            String contenuto
    ) {
        this(partecipazione, contenuto, null);
    }

    private Sottomissione(
            Partecipazione partecipazione,
            String contenuto,
            Long id
    ) {
        this.partecipazione = Objects.requireNonNull(
                partecipazione, "La partecipazione è obbligatoria"
        );

        this.contenuto = validaContenuto(contenuto);

        if (id != null) {
            assegnaId(id);
        }

        partecipazione.registraSottomissione(this);
    }

    public static Sottomissione crea(
            Partecipazione partecipazione,
            String contenuto
    ) {
        return new Sottomissione(partecipazione, contenuto);
    }

    public static Sottomissione ricostruisci(
            Long id,
            Partecipazione partecipazione,
            String contenuto
    ) {
        Objects.requireNonNull(
                id, "L'id della sottomissione è obbligatorio"
        );

        return new Sottomissione(partecipazione, contenuto, id);
    }

    public void assegnaId(Long id) {
        Long idValido = Objects.requireNonNull(
                id, "L'id della sottomissione è obbligatorio"
        );

        if (idValido <= 0) {
            throw new IllegalArgumentException(
                    "L'id della sottomissione deve essere maggiore di zero"
            );
        }

        if (this.id != null) {
            throw new IllegalStateException(
                    "L'id della sottomissione è già stato assegnato"
            );
        }

        this.id = idValido;
    }

    void registraValutazione(Valutazione valutazione) {
        Valutazione valutazioneValida = Objects.requireNonNull(
                valutazione, "La valutazione è obbligatoria"
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

    public void annullaValutazioneNonRegistrata(
            Valutazione valutazione
    ) {
        Objects.requireNonNull(
                valutazione,
                "La valutazione è obbligatoria"
        );

        if (this.valutazione == valutazione) {
            this.valutazione = null;
        }
    }

    public Long getId() {
        return id;
    }

    public String ottieniContenuto() {
        return contenuto;
    }

    public void aggiornaContenuto(String nuovoContenuto) {
        contenuto = validaContenuto(nuovoContenuto);
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

    private static String validaContenuto(String contenuto) {
        if (contenuto == null || contenuto.isBlank()) {
            throw new IllegalArgumentException(
                    "Il contenuto della sottomissione è obbligatorio"
            );
        }

        return contenuto;
    }
}