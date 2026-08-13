package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiValutazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Valutazione;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ValutareSottomissioneControl {

    private final HackathonRepository hackathonRepository;
    private final PartecipazioneRepository partecipazioneRepository;
    private final ValutazioneRepository valutazioneRepository;

    public ValutareSottomissioneControl(
            HackathonRepository hackathonRepository,
            PartecipazioneRepository partecipazioneRepository,
            ValutazioneRepository valutazioneRepository
    ) {
        this.hackathonRepository = Objects.requireNonNull(
                hackathonRepository,
                "Il repository degli hackathon è obbligatorio"
        );

        this.partecipazioneRepository = Objects.requireNonNull(
                partecipazioneRepository,
                "Il repository delle partecipazioni è obbligatorio"
        );

        this.valutazioneRepository = Objects.requireNonNull(
                valutazioneRepository,
                "Il repository delle valutazioni è obbligatorio"
        );
    }

    public List<Hackathon> avviaValutazioneSottomissione(
            Utente giudice
    ) {
        Utente giudiceValido = Objects.requireNonNull(
                giudice,
                "Il giudice è obbligatorio"
        );

        return hackathonRepository.ottieniHackathonValutabili(
                giudiceValido
        );
    }

    public List<Sottomissione> selezionaHackathon(
            Hackathon hackathon
    ) {
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        hackathonValido.aggiornaStato(LocalDate.now());

        if (hackathonValido.getStato()
                != StatoHackathon.IN_VALUTAZIONE) {
            throw new IllegalStateException(
                    "L'hackathon non è in valutazione"
            );
        }

        return partecipazioneRepository
                .ottieniPartecipazioni(hackathonValido)
                .stream()
                .map(Partecipazione::getSottomissione)
                .filter(Objects::nonNull)
                .filter(sottomissione ->
                        sottomissione.getValutazione() == null
                )
                .toList();
    }

    public void selezionaSottomissione(
            Sottomissione sottomissione
    ) {
        Sottomissione sottomissioneValida =
                Objects.requireNonNull(
                        sottomissione,
                        "La sottomissione è obbligatoria"
                );

        if (sottomissioneValida.getValutazione() != null) {
            throw new IllegalStateException(
                    "La sottomissione è già stata valutata"
            );
        }
    }

    public void verificaDatiValutazione(
            DatiValutazione dati
    ) {
        validaCompletezzaEFormato(dati);
    }

    public void confermaValutazione(
            Sottomissione sottomissione,
            Utente giudice,
            DatiValutazione dati
    ) {
        Sottomissione sottomissioneValida =
                Objects.requireNonNull(
                        sottomissione,
                        "La sottomissione è obbligatoria"
                );

        Utente giudiceValido = Objects.requireNonNull(
                giudice,
                "Il giudice è obbligatorio"
        );

        validaCompletezzaEFormato(dati);

        Partecipazione partecipazione =
                sottomissioneValida.getPartecipazione();

        Hackathon hackathon =
                partecipazione.getHackathon();

        hackathon.aggiornaStato(LocalDate.now());

        if (hackathon.getStato()
                != StatoHackathon.IN_VALUTAZIONE) {
            throw new IllegalStateException(
                    "L'hackathon non è in valutazione"
            );
        }

        if (!Objects.equals(
                hackathon.getGiudice().getId(),
                giudiceValido.getId()
        )) {
            throw new IllegalArgumentException(
                    "Il giudice non è assegnato a questo hackathon"
            );
        }

        if (sottomissioneValida.getValutazione() != null) {
            throw new IllegalStateException(
                    "La sottomissione è già stata valutata"
            );
        }

        Valutazione valutazione = Valutazione.crea(
                sottomissioneValida,
                giudiceValido,
                dati
        );

        valutazioneRepository.salva(valutazione);
    }

    private void validaCompletezzaEFormato(
            DatiValutazione dati
    ) {
        DatiValutazione datiValidi =
                Objects.requireNonNull(
                        dati,
                        "I dati della valutazione sono obbligatori"
                );

        if (datiValidi.giudizio() == null
                || datiValidi.giudizio().isBlank()) {
            throw new IllegalArgumentException(
                    "Il giudizio è obbligatorio"
            );
        }

        BigDecimal punteggio = Objects.requireNonNull(
                datiValidi.punteggio(),
                "Il punteggio è obbligatorio"
        );

        if (punteggio.compareTo(BigDecimal.ZERO) < 0
                || punteggio.compareTo(BigDecimal.TEN) > 0) {
            throw new IllegalArgumentException(
                    "Il punteggio deve essere compreso tra 0 e 10"
            );
        }
    }
}