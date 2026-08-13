package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Utente;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CreareHackathonControl {

    private final UtenteRepository utenteRepository;
    private final HackathonRepository hackathonRepository;

    public CreareHackathonControl(
            UtenteRepository utenteRepository,
            HackathonRepository hackathonRepository
    ) {
        this.utenteRepository = Objects.requireNonNull(
                utenteRepository,
                "Il repository degli utenti è obbligatorio"
        );

        this.hackathonRepository = Objects.requireNonNull(
                hackathonRepository,
                "Il repository degli hackathon è obbligatorio"
        );
    }

    public List<Utente> recuperaUtentiAssegnabili() {
        return utenteRepository.recuperaUtentiAssegnabili();
    }

    public List<String> verificaInformazioniEStaff(
            DatiHackathon dati,
            Utente giudice,
            List<Utente> mentori
    ) {
        List<String> errori = new ArrayList<>();

        if (dati == null) {
            errori.add("I dati dell'hackathon sono obbligatori");
            return List.copyOf(errori);
        }

        if (dati.nome() == null || dati.nome().isBlank()) {
            errori.add("Il nome è obbligatorio");
        }

        if (dati.regolamento() == null
                || dati.regolamento().isBlank()) {
            errori.add("Il regolamento è obbligatorio");
        }

        if (dati.criteriValutazione() == null
                || dati.criteriValutazione().isBlank()) {
            errori.add(
                    "I criteri di valutazione sono obbligatori"
            );
        }

        if (dati.luogo() == null || dati.luogo().isBlank()) {
            errori.add("Il luogo è obbligatorio");
        }

        if (dati.scadenzaIscrizioni() == null) {
            errori.add(
                    "La scadenza delle iscrizioni è obbligatoria"
            );
        }

        if (dati.dataInizio() == null) {
            errori.add("La data di inizio è obbligatoria");
        }

        if (dati.dataFine() == null) {
            errori.add("La data di fine è obbligatoria");
        }

        if (dati.scadenzaIscrizioni() != null
                && dati.dataInizio() != null
                && !dati.scadenzaIscrizioni()
                .isBefore(dati.dataInizio())) {
            errori.add(
                    "La scadenza delle iscrizioni deve precedere la data di inizio"
            );
        }

        if (dati.dataInizio() != null
                && dati.dataFine() != null
                && !dati.dataFine().isAfter(dati.dataInizio())) {
            errori.add(
                    "La data di fine deve essere successiva alla data di inizio"
            );
        }

        if (dati.importoPremio() == null) {
            errori.add("L'importo del premio è obbligatorio");
        } else if (dati.importoPremio()
                .compareTo(BigDecimal.ZERO) <= 0) {
            errori.add(
                    "L'importo del premio deve essere maggiore di zero"
            );
        }

        if (dati.dimensioneMassimaTeam() == null) {
            errori.add(
                    "La dimensione massima del team è obbligatoria"
            );
        } else if (dati.dimensioneMassimaTeam() <= 0) {
            errori.add(
                    "La dimensione massima del team deve essere maggiore di zero"
            );
        }

        if (giudice == null) {
            errori.add("Il giudice è obbligatorio");
        }

        if (mentori == null) {
            errori.add("La lista dei mentori è obbligatoria");
        } else if (mentori.isEmpty()) {
            errori.add("Deve essere assegnato almeno un mentore");
        }

        return List.copyOf(errori);
    }

    public void crea(
            DatiHackathon dati,
            Utente organizzatore,
            Utente giudice,
            List<Utente> mentori
    ) {
        Hackathon hackathon = Hackathon.crea(
                dati,
                organizzatore,
                giudice,
                mentori
        );

        hackathonRepository.salva(hackathon);
    }
}