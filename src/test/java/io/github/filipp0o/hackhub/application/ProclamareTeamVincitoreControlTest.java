package io.github.filipp0o.hackhub.application;

import io.github.filipp0o.hackhub.domain.DatiHackathon;
import io.github.filipp0o.hackhub.domain.DatiValutazione;
import io.github.filipp0o.hackhub.domain.Hackathon;
import io.github.filipp0o.hackhub.domain.NotificaSegnalazione;
import io.github.filipp0o.hackhub.domain.Partecipazione;
import io.github.filipp0o.hackhub.domain.RiscossionePremio;
import io.github.filipp0o.hackhub.domain.Segnalazione;
import io.github.filipp0o.hackhub.domain.Sottomissione;
import io.github.filipp0o.hackhub.domain.StatoHackathon;
import io.github.filipp0o.hackhub.domain.StatoRiscossionePremio;
import io.github.filipp0o.hackhub.domain.Team;
import io.github.filipp0o.hackhub.domain.Utente;
import io.github.filipp0o.hackhub.domain.Valutazione;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProclamareTeamVincitoreControlTest {

    @Test
    void rifiutaDipendenzeNulle() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ProclamareTeamVincitoreControl(
                                null,
                                new HackathonRepositoryFinto(),
                                new SegnalazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ProclamareTeamVincitoreControl(
                                new PartecipazioneRepositoryFinto(),
                                null,
                                new SegnalazioneRepositoryFinto()
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new ProclamareTeamVincitoreControl(
                                new PartecipazioneRepositoryFinto(),
                                new HackathonRepositoryFinto(),
                                null
                        )
                )
        );
    }

    @Test
    void rifiutaParametriNulli() {
        Utente organizzatore = new Utente(1L);
        Hackathon hackathon =
                creaHackathonInValutazione(organizzatore);

        Partecipazione partecipazione =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        new PartecipazioneRepositoryFinto(),
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.avviaProclamazioneTeamVincitore(
                                null,
                                hackathon
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.avviaProclamazioneTeamVincitore(
                                organizzatore,
                                null
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.preparaProclamazione(
                                null,
                                partecipazione
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.preparaProclamazione(
                                hackathon,
                                null
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.confermaProclamazione(
                                null,
                                hackathon,
                                partecipazione
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.confermaProclamazione(
                                organizzatore,
                                null,
                                partecipazione
                        )
                ),
                () -> assertThrows(
                        NullPointerException.class,
                        () -> control.confermaProclamazione(
                                organizzatore,
                                hackathon,
                                null
                        )
                )
        );
    }

    @Test
    void avviaProclamazioneRestituiscePartecipazioniNonEscluse() {
        Utente organizzatoreAssegnato = new Utente(1L);
        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatoreAssegnato
                );

        Partecipazione prima =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );
        Partecipazione seconda =
                creaPartecipazioneValutata(
                        hackathon,
                        11L
                );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazioni =
                List.of(prima, seconda);
        partecipazioneRepository.partecipazioniNonEscluse =
                List.of(prima, seconda);

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        /*
         * Istanza diversa, ma stesso identificatore:
         * rappresenta lo stesso organizzatore recuperato
         * in un altro contesto.
         */
        Utente organizzatore = new Utente(1L);

        List<Partecipazione> risultato =
                control.avviaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                );

        assertAll(
                () -> assertEquals(
                        List.of(prima, seconda),
                        risultato
                ),
                () -> assertSame(
                        hackathon,
                        partecipazioneRepository.hackathonRicevuto
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperi
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperiNonEscluse
                ),
                () -> assertEquals(
                        StatoHackathon.IN_VALUTAZIONE,
                        hackathon.getStato()
                )
        );
    }

    @Test
    void nonAvviaProclamazioneSeOrganizzatoreNonAssegnato() {
        Utente organizzatoreAssegnato = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatoreAssegnato
                );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        Utente altroOrganizzatore = new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.avviaProclamazioneTeamVincitore(
                        altroOrganizzatore,
                        hackathon
                )
        );

        assertEquals(
                0,
                partecipazioneRepository.numeroRecuperiNonEscluse
        );
    }

    @Test
    void nonAvviaProclamazioneSeHackathonNonInValutazione() {
        Utente organizzatore = new Utente(1L);

        Hackathon hackathon =
                creaHackathonFuturo(
                        organizzatore
                );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        StatoHackathon.IN_ISCRIZIONE,
                        hackathon.getStato()
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroRecuperiNonEscluse
                )
        );
    }

    @Test
    void nonAvviaProclamazioneSenzaSottomissioni() {
        Utente organizzatore = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatore
                );

        Partecipazione partecipazione =
                creaPartecipazione(
                        hackathon,
                        10L
                );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazioni =
                List.of(partecipazione);
        partecipazioneRepository.partecipazioniNonEscluse =
                List.of(partecipazione);

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperi
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperiNonEscluse
                )
        );
    }

    @Test
    void nonAvviaProclamazioneSeUnaSottomissioneNonEValutata() {
        Utente organizzatore = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatore
                );

        Partecipazione valutata =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );

        Partecipazione nonValutata =
                creaPartecipazione(
                        hackathon,
                        11L
                );

        new Sottomissione(
                nonValutata,
                "Sottomissione non ancora valutata"
        );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazioni =
                List.of(
                        valutata,
                        nonValutata
                );
        partecipazioneRepository.partecipazioniNonEscluse =
                List.of(
                        valutata,
                        nonValutata
                );

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperi
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroRecuperiNonEscluse
                )
        );
    }

    @Test
    void nonAvviaProclamazioneSeSottomissioneEsclusaNonEValutata() {
        Utente organizzatore = new Utente(1L);
        Hackathon hackathon =
                creaHackathonInValutazione(organizzatore);

        Partecipazione valutata =
                creaPartecipazioneValutata(hackathon, 10L);

        Partecipazione esclusaNonValutata =
                creaPartecipazione(hackathon, 11L);

        new Sottomissione(
                esclusaNonValutata,
                "Sottomissione esclusa non valutata"
        );
        esclusaNonValutata.escludi();

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();

        partecipazioneRepository.partecipazioni =
                List.of(valutata, esclusaNonValutata);
        partecipazioneRepository.partecipazioniNonEscluse =
                List.of(valutata);

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperi
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroRecuperiNonEscluse
                )
        );
    }

    @Test
    void nonAvviaProclamazioneConSegnalazioneDaEsaminare() {
        Utente organizzatore = new Utente(1L);
        Hackathon hackathon =
                creaHackathonInValutazione(organizzatore);

        Partecipazione partecipazione =
                creaPartecipazioneValutata(hackathon, 10L);

        Segnalazione segnalazione = Segnalazione.crea(
                new Utente(3L),
                partecipazione,
                "Violazione da esaminare"
        );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();
        partecipazioneRepository.partecipazioni =
                List.of(partecipazione);
        partecipazioneRepository.partecipazioniNonEscluse =
                List.of(partecipazione);

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();
        segnalazioneRepository.segnalazioniDaEsaminare =
                List.of(segnalazione);

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new HackathonRepositoryFinto(),
                        segnalazioneRepository
                );

        assertThrows(
                IllegalStateException.class,
                () -> control.avviaProclamazioneTeamVincitore(
                        organizzatore,
                        hackathon
                )
        );

        assertAll(
                () -> assertSame(
                        organizzatore,
                        segnalazioneRepository.organizzatoreRicevuto
                ),
                () -> assertEquals(
                        1,
                        segnalazioneRepository.numeroRecuperi
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroRecuperi
                ),
                () -> assertEquals(
                        0,
                        partecipazioneRepository.numeroRecuperiNonEscluse
                )
        );
    }

    @Test
    void segnalazioneDiAltroHackathonNonBloccaProclamazione() {
        Utente organizzatore = new Utente(1L);
        Hackathon hackathon =
                creaHackathonInValutazione(organizzatore);
        Hackathon altroHackathon =
                creaHackathonInValutazione(new Utente(1L));

        Partecipazione partecipazione =
                creaPartecipazioneValutata(hackathon, 10L);
        Partecipazione partecipazioneAltroHackathon =
                creaPartecipazioneValutata(altroHackathon, 11L);

        Segnalazione segnalazioneAltroHackathon =
                Segnalazione.crea(
                        new Utente(3L),
                        partecipazioneAltroHackathon,
                        "Violazione di un altro hackathon"
                );

        PartecipazioneRepositoryFinto partecipazioneRepository =
                new PartecipazioneRepositoryFinto();
        partecipazioneRepository.partecipazioni =
                List.of(partecipazione);
        partecipazioneRepository.partecipazioniNonEscluse =
                List.of(partecipazione);

        SegnalazioneRepositoryFinto segnalazioneRepository =
                new SegnalazioneRepositoryFinto();
        segnalazioneRepository.segnalazioniDaEsaminare =
                List.of(segnalazioneAltroHackathon);

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        partecipazioneRepository,
                        new HackathonRepositoryFinto(),
                        segnalazioneRepository
                );

        List<Partecipazione> risultato =
                assertDoesNotThrow(
                        () -> control.avviaProclamazioneTeamVincitore(
                                organizzatore,
                                hackathon
                        )
                );

        assertAll(
                () -> assertEquals(
                        List.of(partecipazione),
                        risultato
                ),
                () -> assertEquals(
                        1,
                        segnalazioneRepository.numeroRecuperi
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperi
                ),
                () -> assertEquals(
                        1,
                        partecipazioneRepository.numeroRecuperiNonEscluse
                )
        );
    }

    @Test
    void preparaProclamazioneConPartecipazioneValida() {
        Utente organizzatore = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatore
                );

        Partecipazione partecipazione =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );

        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        new PartecipazioneRepositoryFinto(),
                        hackathonRepository,
                        new SegnalazioneRepositoryFinto()
                );

        assertDoesNotThrow(
                () -> control.preparaProclamazione(
                        hackathon,
                        partecipazione
                )
        );

        assertAll(
                () -> assertNull(
                        hackathon.getVincitrice()
                ),
                () -> assertNull(
                        hackathon.getRiscossionePremio()
                ),
                () -> assertEquals(
                        StatoHackathon.IN_VALUTAZIONE,
                        hackathon.getStato()
                ),
                () -> assertEquals(
                        0,
                        hackathonRepository.numeroSalvataggi
                )
        );
    }

    @Test
    void rifiutaPartecipazioneSelezionataDiAltroHackathon() {
        Utente primoOrganizzatore = new Utente(1L);
        Utente secondoOrganizzatore = new Utente(4L);

        Hackathon primoHackathon =
                creaHackathonInValutazione(
                        primoOrganizzatore
                );

        Hackathon secondoHackathon =
                creaHackathonInValutazione(
                        secondoOrganizzatore
                );

        Partecipazione partecipazioneAltroHackathon =
                creaPartecipazioneValutata(
                        secondoHackathon,
                        10L
                );

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        new PartecipazioneRepositoryFinto(),
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> control.preparaProclamazione(
                        primoHackathon,
                        partecipazioneAltroHackathon
                )
        );
    }

    @Test
    void rifiutaPartecipazioneSelezionataEsclusa() {
        Utente organizzatore = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatore
                );

        Partecipazione partecipazione =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );

        partecipazione.escludi();

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        new PartecipazioneRepositoryFinto(),
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> control.preparaProclamazione(
                        hackathon,
                        partecipazione
                )
        );
    }

    @Test
    void rifiutaPartecipazioneSenzaSottomissioneONonValutata() {
        Utente organizzatore = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatore
                );

        Partecipazione senzaSottomissione =
                creaPartecipazione(
                        hackathon,
                        10L
                );

        Partecipazione nonValutata =
                creaPartecipazione(
                        hackathon,
                        11L
                );

        new Sottomissione(
                nonValutata,
                "Sottomissione non valutata"
        );

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        new PartecipazioneRepositoryFinto(),
                        new HackathonRepositoryFinto(),
                        new SegnalazioneRepositoryFinto()
                );

        assertAll(
                () -> assertThrows(
                        IllegalArgumentException.class,
                        () -> control.preparaProclamazione(
                                hackathon,
                                senzaSottomissione
                        )
                ),
                () -> assertThrows(
                        IllegalStateException.class,
                        () -> control.preparaProclamazione(
                                hackathon,
                                nonValutata
                        )
                )
        );
    }

    @Test
    void confermaProclamazioneEInizializzaRiscossione() {
        Utente organizzatoreAssegnato = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatoreAssegnato
                );

        Partecipazione partecipazioneVincitrice =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );

        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        new PartecipazioneRepositoryFinto(),
                        hackathonRepository,
                        new SegnalazioneRepositoryFinto()
                );

        /*
         * Stesso organizzatore rappresentato
         * da un'altra istanza con lo stesso id.
         */
        Utente organizzatore = new Utente(1L);

        control.confermaProclamazione(
                organizzatore,
                hackathon,
                partecipazioneVincitrice
        );

        RiscossionePremio riscossione =
                hackathon.getRiscossionePremio();

        assertAll(
                () -> assertSame(
                        partecipazioneVincitrice,
                        hackathon.getVincitrice()
                ),
                () -> assertEquals(
                        StatoHackathon.CONCLUSO,
                        hackathon.getStato()
                ),
                () -> assertNotNull(
                        riscossione
                ),
                () -> assertEquals(
                        StatoRiscossionePremio.DA_CONFIGURARE,
                        riscossione.getStato()
                ),
                () -> assertSame(
                        hackathon,
                        riscossione.getHackathon()
                ),
                () -> assertNull(
                        riscossione.getBeneficiaryRef()
                ),
                () -> assertNull(
                        riscossione.getPaymentRef()
                ),
                () -> assertSame(
                        hackathon,
                        hackathonRepository.hackathonSalvato
                ),
                () -> assertEquals(
                        1,
                        hackathonRepository.numeroSalvataggi
                )
        );
    }

    @Test
    void nonConfermaProclamazioneSeOrganizzatoreNonAssegnato() {
        Utente organizzatoreAssegnato = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatoreAssegnato
                );

        Partecipazione partecipazione =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );

        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        new PartecipazioneRepositoryFinto(),
                        hackathonRepository,
                        new SegnalazioneRepositoryFinto()
                );

        Utente altroOrganizzatore = new Utente(99L);

        assertThrows(
                IllegalArgumentException.class,
                () -> control.confermaProclamazione(
                        altroOrganizzatore,
                        hackathon,
                        partecipazione
                )
        );

        assertAll(
                () -> assertNull(
                        hackathon.getVincitrice()
                ),
                () -> assertNull(
                        hackathon.getRiscossionePremio()
                ),
                () -> assertEquals(
                        StatoHackathon.IN_VALUTAZIONE,
                        hackathon.getStato()
                ),
                () -> assertEquals(
                        0,
                        hackathonRepository.numeroSalvataggi
                )
        );
    }

    @Test
    void nonConfermaSePartecipazioneDiventaEsclusa() {
        Utente organizzatore = new Utente(1L);

        Hackathon hackathon =
                creaHackathonInValutazione(
                        organizzatore
                );

        Partecipazione partecipazione =
                creaPartecipazioneValutata(
                        hackathon,
                        10L
                );

        HackathonRepositoryFinto hackathonRepository =
                new HackathonRepositoryFinto();

        ProclamareTeamVincitoreControl control =
                new ProclamareTeamVincitoreControl(
                        new PartecipazioneRepositoryFinto(),
                        hackathonRepository,
                        new SegnalazioneRepositoryFinto()
                );

        control.preparaProclamazione(
                hackathon,
                partecipazione
        );

        partecipazione.escludi();

        assertThrows(
                IllegalArgumentException.class,
                () -> control.confermaProclamazione(
                        organizzatore,
                        hackathon,
                        partecipazione
                )
        );

        assertAll(
                () -> assertNull(
                        hackathon.getVincitrice()
                ),
                () -> assertNull(
                        hackathon.getRiscossionePremio()
                ),
                () -> assertEquals(
                        StatoHackathon.IN_VALUTAZIONE,
                        hackathon.getStato()
                ),
                () -> assertEquals(
                        0,
                        hackathonRepository.numeroSalvataggi
                )
        );
    }

    private Hackathon creaHackathonInValutazione(
            Utente organizzatore
    ) {
        Hackathon hackathon = Hackathon.crea(
                creaDatiHackathonPassato(),
                organizzatore,
                new Utente(2L),
                List.of(new Utente(3L))
        );

        hackathon.aggiornaStato(LocalDate.now());

        return hackathon;
    }

    private Hackathon creaHackathonFuturo(
            Utente organizzatore
    ) {
        DatiHackathon dati = new DatiHackathon(
                "Hackathon futuro",
                "Regolamento",
                "Qualità, completezza e innovazione",
                LocalDate.of(2999, 1, 1),
                LocalDate.of(2999, 1, 10),
                LocalDate.of(2999, 1, 12),
                "Roma",
                BigDecimal.valueOf(5000),
                5
        );

        return Hackathon.crea(
                dati,
                organizzatore,
                new Utente(2L),
                List.of(new Utente(3L))
        );
    }

    private DatiHackathon creaDatiHackathonPassato() {
        return new DatiHackathon(
                "Hackathon passato",
                "Regolamento",
                "Qualità, completezza e innovazione",
                LocalDate.of(2020, 1, 1),
                LocalDate.of(2020, 1, 10),
                LocalDate.of(2020, 1, 12),
                "Roma",
                BigDecimal.valueOf(5000),
                5
        );
    }

    private Partecipazione creaPartecipazione(
            Hackathon hackathon,
            long idResponsabile
    ) {
        Utente responsabile =
                new Utente(idResponsabile);

        Team team = Team.crea(
                "Team " + idResponsabile,
                responsabile,
                responsabile
        );

        return new Partecipazione(
                hackathon,
                team
        );
    }

    private Partecipazione creaPartecipazioneValutata(
            Hackathon hackathon,
            long idResponsabile
    ) {
        Partecipazione partecipazione =
                creaPartecipazione(
                        hackathon,
                        idResponsabile
                );

        Sottomissione sottomissione =
                new Sottomissione(
                        partecipazione,
                        "Sottomissione del team " + idResponsabile
                );

        Valutazione.crea(
                sottomissione,
                hackathon.getGiudice(),
                creaDatiValutazioneValidi()
        );

        return partecipazione;
    }

    private DatiValutazione creaDatiValutazioneValidi() {
        return new DatiValutazione(
                "Buona sottomissione",
                BigDecimal.valueOf(8)
        );
    }

    private static class PartecipazioneRepositoryFinto
            implements PartecipazioneRepository {

        private List<Partecipazione> partecipazioni =
                List.of();
        private List<Partecipazione> partecipazioniNonEscluse =
                List.of();

        private Hackathon hackathonRicevuto;
        private int numeroRecuperi;
        private int numeroRecuperiNonEscluse;

        @Override
        public List<Partecipazione> ottieniPartecipazioni(
                Hackathon hackathon
        ) {
            hackathonRicevuto = hackathon;
            numeroRecuperi++;

            return partecipazioni;
        }

        @Override
        public List<Partecipazione> recuperaPartecipazioniNonEscluse(
                Hackathon hackathon
        ) {
            hackathonRicevuto = hackathon;
            numeroRecuperiNonEscluse++;

            return partecipazioniNonEscluse;
        }

        @Override
        public void salva(
                Partecipazione partecipazione
        ) {
        }

        @Override
        public boolean esistePartecipazione(
                Team team,
                Hackathon hackathon
        ) {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }

    private static class SegnalazioneRepositoryFinto
            implements SegnalazioneRepository {

        private List<Segnalazione> segnalazioniDaEsaminare =
                List.of();

        private Utente organizzatoreRicevuto;
        private int numeroRecuperi;

        @Override
        public List<Segnalazione> ottieniSegnalazioniDaEsaminare(
                Utente organizzatore
        ) {
            organizzatoreRicevuto = organizzatore;
            numeroRecuperi++;

            return segnalazioniDaEsaminare;
        }

        @Override
        public void salva(
                Segnalazione segnalazione
        ) {
        }

        @Override
        public void salvaConNotifica(
                Segnalazione segnalazione,
                NotificaSegnalazione notifica
        ) {
        }

        @Override
        public void salvaNotifica(
                NotificaSegnalazione notifica
        ) {
        }
    }

    private static class HackathonRepositoryFinto
            implements HackathonRepository {

        private Hackathon hackathonSalvato;
        private int numeroSalvataggi;

        @Override
        public List<Hackathon> ottieniHackathonValutabili(
                Utente giudice
        ) {
            return List.of();
        }

        @Override
        public List<Hackathon> ottieniHackathonSegnalabili(
                Utente mentore
        ) {
            return List.of();
        }

        @Override
        public void salva(
                Hackathon hackathon
        ) {
            hackathonSalvato = hackathon;
            numeroSalvataggi++;
        }

        @Override
        public List<Hackathon> ottieniHackathonApertiAlleIscrizioni() {
            throw new UnsupportedOperationException(
                    "Non utilizzato in questo test"
            );
        }
    }
}