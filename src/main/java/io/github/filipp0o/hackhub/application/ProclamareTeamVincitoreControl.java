package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.StatoPartecipazione;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Segnalazione;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public class ProclamareTeamVincitoreControl {

    private final PartecipazioneRepository partecipazioneRepository;
    private final HackathonRepository hackathonRepository;
    private final SegnalazioneRepository segnalazioneRepository;

    public ProclamareTeamVincitoreControl(
            PartecipazioneRepository partecipazioneRepository,
            HackathonRepository hackathonRepository,
            SegnalazioneRepository segnalazioneRepository
    ) {
        this.partecipazioneRepository = Objects.requireNonNull(
                partecipazioneRepository,
                "Il repository delle partecipazioni è obbligatorio"
        );

        this.hackathonRepository = Objects.requireNonNull(
                hackathonRepository,
                "Il repository degli hackathon è obbligatorio"
        );

        this.segnalazioneRepository = Objects.requireNonNull(
                segnalazioneRepository,
                "Il repository delle segnalazioni è obbligatorio"
        );
    }

    public List<Partecipazione> avviaProclamazioneTeamVincitore(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        Utente organizzatoreValido = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );

        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );

        hackathonValido.aggiornaStato(LocalDate.now());

        verificaHackathonInValutazione(hackathonValido);
        verificaOrganizzatore(
                organizzatoreValido,
                hackathonValido
        );

        List<Segnalazione> segnalazioniDaEsaminare =
                segnalazioneRepository
                        .ottieniSegnalazioniDaEsaminare(
                                organizzatoreValido
                        );

        verificaAssenzaSegnalazioniDaEsaminare(
                hackathonValido,
                segnalazioniDaEsaminare
        );

        List<Partecipazione> partecipazioni =
                partecipazioneRepository
                        .ottieniPartecipazioni(
                                hackathonValido
                        );

        verificaSottomissioniValutate(
                partecipazioni
        );

        List<Partecipazione> partecipazioniNonEscluse =
                partecipazioneRepository
                        .recuperaPartecipazioniNonEscluse(
                                hackathonValido
                        );

        verificaEsistenzaSottomissioneAmmissibile(
                partecipazioniNonEscluse
        );

        return partecipazioniNonEscluse;
    }

    public void preparaProclamazione(
            Hackathon hackathon,
            Partecipazione partecipazioneSelezionata
    ) {
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );
        Partecipazione partecipazioneValida =
                Objects.requireNonNull(
                        partecipazioneSelezionata,
                        "La partecipazione selezionata è obbligatoria"
                );

        hackathonValido.aggiornaStato(LocalDate.now());

        verificaHackathonInValutazione(hackathonValido);
        verificaPartecipazioneSelezionata(
                hackathonValido,
                partecipazioneValida
        );
    }

    public void confermaProclamazione(
            Utente organizzatore,
            Hackathon hackathon,
            Partecipazione partecipazioneSelezionata
    ) {
        Utente organizzatoreValido = Objects.requireNonNull(
                organizzatore,
                "L'organizzatore è obbligatorio"
        );
        Hackathon hackathonValido = Objects.requireNonNull(
                hackathon,
                "L'hackathon è obbligatorio"
        );
        Partecipazione partecipazioneValida =
                Objects.requireNonNull(
                        partecipazioneSelezionata,
                        "La partecipazione selezionata è obbligatoria"
                );

        hackathonValido.aggiornaStato(LocalDate.now());

        verificaHackathonInValutazione(hackathonValido);
        verificaOrganizzatore(
                organizzatoreValido,
                hackathonValido
        );
        verificaPartecipazioneSelezionata(
                hackathonValido,
                partecipazioneValida
        );

        hackathonValido.registraPartecipazioneVincitrice(
                partecipazioneValida
        );

        hackathonValido.concludi();

        RiscossionePremio.crea(hackathonValido);

        hackathonRepository.salva(hackathonValido);
    }

    private void verificaHackathonInValutazione(
            Hackathon hackathon
    ) {
        if (hackathon.getStato()
                != StatoHackathon.IN_VALUTAZIONE) {
            throw new IllegalStateException(
                    "L'hackathon non è in valutazione"
            );
        }
    }

    private void verificaOrganizzatore(
            Utente organizzatore,
            Hackathon hackathon
    ) {
        if (!Objects.equals(
                hackathon.getOrganizzatore().getId(),
                organizzatore.getId()
        )) {
            throw new IllegalArgumentException(
                    "L'organizzatore non è assegnato a questo hackathon"
            );
        }
    }

    private void verificaSottomissioniValutate(
            List<Partecipazione> partecipazioni
    ) {
        boolean esisteSottomissioneNonValutata =
                partecipazioni.stream()
                        .map(Partecipazione::getSottomissione)
                        .filter(Objects::nonNull)
                        .anyMatch(sottomissione ->
                                sottomissione.getValutazione() == null
                        );

        if (esisteSottomissioneNonValutata) {
            throw new IllegalStateException(
                    "Non tutte le sottomissioni sono state valutate"
            );
        }
    }

    private void verificaPartecipazioneSelezionata(
            Hackathon hackathon,
            Partecipazione partecipazione
    ) {
        if (partecipazione.getHackathon() != hackathon) {
            throw new IllegalArgumentException(
                    "La partecipazione selezionata non appartiene a questo hackathon"
            );
        }

        if (partecipazione.getStato()
                != StatoPartecipazione.ATTIVA) {
            throw new IllegalArgumentException(
                    "La partecipazione selezionata è esclusa"
            );
        }

        Sottomissione sottomissione =
                partecipazione.getSottomissione();

        if (sottomissione == null) {
            throw new IllegalArgumentException(
                    "Il team selezionato non possiede una sottomissione"
            );
        }

        if (sottomissione.getValutazione() == null) {
            throw new IllegalStateException(
                    "La sottomissione del team selezionato non è stata valutata"
            );
        }
    }

    private void verificaAssenzaSegnalazioniDaEsaminare(
            Hackathon hackathon,
            List<Segnalazione> segnalazioni
    ) {
        boolean esisteSegnalazioneDaEsaminare =
                segnalazioni.stream()
                        .map(Segnalazione::getPartecipazione)
                        .anyMatch(partecipazione ->
                                partecipazione.getHackathon()
                                        == hackathon
                        );

        if (esisteSegnalazioneDaEsaminare) {
            throw new IllegalStateException(
                    "Esistono segnalazioni ancora da esaminare per l'hackathon"
            );
        }
    }

    private void verificaEsistenzaSottomissioneAmmissibile(
            List<Partecipazione> partecipazioni
    ) {
        boolean esisteSottomissioneAmmissibile =
                partecipazioni.stream()
                        .map(Partecipazione::getSottomissione)
                        .filter(Objects::nonNull)
                        .anyMatch(sottomissione ->
                                sottomissione.getValutazione() != null
                        );

        if (!esisteSottomissioneAmmissibile) {
            throw new IllegalStateException(
                    "Non esistono sottomissioni valutate appartenenti a partecipazioni attive"
            );
        }
    }
}