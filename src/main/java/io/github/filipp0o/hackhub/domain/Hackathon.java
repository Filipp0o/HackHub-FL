package io.github.filipp0o.hackhub.domain;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class Hackathon {

    private Long id;

    private final String nome;
    private final String regolamento;
    private final String criteriValutazione;
    private final LocalDate scadenzaIscrizioni;
    private final LocalDate dataInizio;
    private final LocalDate dataFine;
    private final String luogo;
    private final BigDecimal importoPremio;
    private final Integer dimensioneMassimaTeam;

    private StatoHackathon stato;
    private Partecipazione vincitrice;
    private RiscossionePremio riscossionePremio;

    private final Utente organizzatore;
    private final Utente giudice;
    private final List<Utente> mentori;

    private Hackathon(
            DatiHackathon dati,
            Utente organizzatore,
            Utente giudice,
            List<Utente> mentori
    ) {
        Objects.requireNonNull(
                dati,
                "I dati dell'hackathon sono obbligatori"
        );

        this.nome = richiediTesto(
                dati.nome(),
                "Il nome è obbligatorio"
        );
        this.regolamento = richiediTesto(
                dati.regolamento(),
                "Il regolamento è obbligatorio"
        );
        this.criteriValutazione = richiediTesto(
                dati.criteriValutazione(),
                "I criteri di valutazione sono obbligatori"
        );
        this.luogo = richiediTesto(
                dati.luogo(),
                "Il luogo è obbligatorio"
        );

        this.scadenzaIscrizioni = Objects.requireNonNull(
                dati.scadenzaIscrizioni(),
                "La scadenza delle iscrizioni è obbligatoria"
        );
        this.dataInizio = Objects.requireNonNull(
                dati.dataInizio(),
                "La data di inizio è obbligatoria"
        );
        this.dataFine = Objects.requireNonNull(
                dati.dataFine(),
                "La data di fine è obbligatoria"
        );
        this.importoPremio = Objects.requireNonNull(
                dati.importoPremio(),
                "L'importo del premio è obbligatorio"
        );
        this.dimensioneMassimaTeam = Objects.requireNonNull(
                dati.dimensioneMassimaTeam(),
                "La dimensione massima del team è obbligatoria"
        );

        if (!scadenzaIscrizioni.isBefore(dataInizio)) {
            throw new IllegalArgumentException(
                    "La scadenza delle iscrizioni deve precedere la data di inizio"
            );
        }

        if (!dataFine.isAfter(dataInizio)) {
            throw new IllegalArgumentException(
                    "La data di fine deve essere successiva alla data di inizio"
            );
        }

        if (importoPremio.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "L'importo del premio deve essere maggiore di zero"
            );
        }

        if (dimensioneMassimaTeam <= 0) {
            throw new IllegalArgumentException(
                    "La dimensione massima del team deve essere maggiore di zero"
            );
        }

        this.organizzatore = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );
        this.giudice = Objects.requireNonNull(
                giudice,
                "Il giudice è obbligatorio"
        );

        Objects.requireNonNull(
                mentori,
                "La lista dei mentori è obbligatoria"
        );

        if (mentori.isEmpty()) {
            throw new IllegalArgumentException(
                    "Deve essere assegnato almeno un mentore"
            );
        }

        this.mentori = List.copyOf(mentori);
        this.stato = StatoHackathon.IN_ISCRIZIONE;
    }

    public static Hackathon crea(
            DatiHackathon dati,
            Utente organizzatore,
            Utente giudice,
            List<Utente> mentori
    ) {
        return new Hackathon(
                dati,
                organizzatore,
                giudice,
                mentori
        );
    }

    private static String richiediTesto(
            String valore,
            String messaggio
    ) {
        if (valore == null || valore.isBlank()) {
            throw new IllegalArgumentException(messaggio);
        }

        return valore;
    }

    public void registraPartecipazioneVincitrice(
            Partecipazione partecipazione
    ) {
        if (stato != StatoHackathon.IN_VALUTAZIONE) {
            throw new IllegalStateException(
                    "Il vincitore può essere registrato solo durante la valutazione"
            );
        }

        if (vincitrice != null) {
            throw new IllegalStateException(
                    "La partecipazione vincitrice è già stata registrata"
            );
        }

        Partecipazione partecipazioneValida =
                Objects.requireNonNull(
                        partecipazione,
                        "La partecipazione vincitrice è obbligatoria"
                );

        if (partecipazioneValida.getStato()
                != StatoPartecipazione.ATTIVA) {
            throw new IllegalArgumentException(
                    "La partecipazione vincitrice deve essere attiva"
            );
        }

        if (partecipazioneValida.getHackathon() != this) {
            throw new IllegalArgumentException(
                    "La partecipazione vincitrice deve appartenere a questo hackathon"
            );
        }

        this.vincitrice = partecipazioneValida;
    }

    public void concludi() {
        if (stato != StatoHackathon.IN_VALUTAZIONE) {
            throw new IllegalStateException(
                    "Può essere concluso solo un hackathon in valutazione"
            );
        }

        if (vincitrice == null) {
            throw new IllegalStateException(
                    "È necessario registrare la partecipazione vincitrice"
            );
        }

        this.stato = StatoHackathon.CONCLUSO;
    }

    void registraRiscossionePremio(
            RiscossionePremio riscossionePremio
    ) {
        RiscossionePremio riscossioneValida =
                Objects.requireNonNull(
                        riscossionePremio,
                        "La riscossione del premio è obbligatoria"
                );

        if (this.riscossionePremio != null) {
            throw new IllegalStateException(
                    "L'hackathon possiede già una riscossione del premio"
            );
        }

        if (riscossioneValida.getHackathon() != this) {
            throw new IllegalArgumentException(
                    "La riscossione deve riferirsi a questo hackathon"
            );
        }

        this.riscossionePremio = riscossioneValida;
    }

    public Long getId() {
        return id;
    }

    public String getNome() {
        return nome;
    }

    public String getRegolamento() {
        return regolamento;
    }

    public String getCriteriValutazione() {
        return criteriValutazione;
    }

    public LocalDate getScadenzaIscrizioni() {
        return scadenzaIscrizioni;
    }

    public LocalDate getDataInizio() {
        return dataInizio;
    }

    public LocalDate getDataFine() {
        return dataFine;
    }

    public String getLuogo() {
        return luogo;
    }

    public BigDecimal getImportoPremio() {
        return importoPremio;
    }

    public Integer getDimensioneMassimaTeam() {
        return dimensioneMassimaTeam;
    }

    public StatoHackathon getStato() {
        return stato;
    }

    public Utente getOrganizzatore() {
        return organizzatore;
    }

    public Utente getGiudice() {
        return giudice;
    }

    public List<Utente> getMentori() {
        return mentori;
    }

    public Partecipazione getVincitrice() {
        return vincitrice;
    }

    public RiscossionePremio getRiscossionePremio() {
        return riscossionePremio;
    }
}